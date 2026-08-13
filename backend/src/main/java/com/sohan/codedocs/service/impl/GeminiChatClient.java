package com.sohan.codedocs.service.impl;

import com.sohan.codedocs.config.properties.GeminiProperties;
import com.sohan.codedocs.exception.AiProviderException;
import com.sohan.codedocs.exception.RateLimitException;
import com.sohan.codedocs.service.ChatClient;
import com.sohan.codedocs.service.ChatTurn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class GeminiChatClient implements ChatClient {

    private static final String SAFETY_BLOCKED = "SAFETY";
    private static final String MAX_TOKENS_REACHED = "MAX_TOKENS";
    private static final String NO_ANSWER_FALLBACK =
            "I can't answer that based on this repository's content.";

    private final RestClient restClient;
    private final GeminiProperties props;
    private final ObjectMapper objectMapper;

    public GeminiChatClient(@Qualifier("geminiRestClient") RestClient restClient,
                            GeminiProperties props, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    @Retryable(
            includes = {RateLimitException.class, ResourceAccessException.class},
            maxRetries = 2,
            delayString = "1500ms",
            multiplier = 2
    )
    public String complete(String systemPrompt, List<ChatTurn> history, String userPrompt) {
        GenerateResponse response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", props.chatModel())
                .body(requestBody(systemPrompt, history, userPrompt))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    if (res.getStatusCode().value() == 429) {
                        throw new RateLimitException("Gemini chat quota exceeded");
                    }
                    String error = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    log.error("Gemini chat error {}: {}", res.getStatusCode(), error);
                    throw new AiProviderException(
                            "Gemini rejected the chat request: " + res.getStatusCode(), null);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    // 5xx is transient — map to the retryable exception.
                    throw new RateLimitException("Gemini unavailable: " + res.getStatusCode());
                })
                .body(GenerateResponse.class);

        return extractText(response);
    }

    /**
     * No @Retryable here, unlike complete() above: onToken is a side effect
     * that has already reached the client by the time a mid-stream failure
     * could occur. An automatic retry would replay the whole answer from the
     * start and duplicate everything already streamed out. A failure here
     * simply ends the stream; the caller reports it once, not resends it.
     */
    @Override
    public void completeStream(String systemPrompt, List<ChatTurn> history, String userPrompt, Consumer<String> onToken) {
        restClient.post()
                .uri("/v1beta/models/{model}:streamGenerateContent?alt=sse", props.chatModel())
                .body(requestBody(systemPrompt, history, userPrompt))
                .exchange((req, res) -> {
                    if (res.getStatusCode().value() == 429) {
                        throw new RateLimitException("Gemini chat quota exceeded");
                    }
                    if (res.getStatusCode().is4xxClientError()) {
                        String error = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.error("Gemini chat stream error {}: {}", res.getStatusCode(), error);
                        throw new AiProviderException(
                                "Gemini rejected the chat request: " + res.getStatusCode(), null);
                    }
                    if (res.getStatusCode().is5xxServerError()) {
                        throw new RateLimitException("Gemini unavailable: " + res.getStatusCode());
                    }
                    readSseStream(res.getBody(), onToken);
                    return null;
                });
    }

    /**
     * history's turns are dropped in verbatim, oldest first, then the new
     * question — Gemini's multi-turn "contents" array is just an ordered
     * user/model transcript, so a follow-up like "what about tests?" is
     * resolved by the model actually seeing what came before it.
     */
    private Map<String, Object> requestBody(String systemPrompt, List<ChatTurn> history, String userPrompt) {
        List<Map<String, Object>> contents = new ArrayList<>(history.size() + 1);
        for (ChatTurn turn : history) {
            contents.add(Map.of("role", turn.role(), "parts", List.of(Map.of("text", turn.text()))));
        }
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt))));

        return Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))),
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", props.temperature(),
                        "maxOutputTokens", props.maxOutputTokens()));
    }

    /**
     * Gemini's SSE stream is a sequence of "data: {json}" lines, each one a
     * partial GenerateResponse — same shape as the non-streaming response,
     * just with one incremental chunk of text per candidate instead of the
     * whole answer. HttpURLConnection (behind SimpleClientHttpRequestFactory)
     * hands back chunked bodies progressively, so reading line-by-line here
     * yields text as Gemini produces it rather than only once the response
     * fully closes.
     */
    private void readSseStream(InputStream body, Consumer<String> onToken) {
        boolean[] emittedAny = {false};
        String[] lastFinishReason = {null};

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String json = line.substring(5).trim();
                if (json.isEmpty() || "[DONE]".equals(json)) continue;

                GenerateResponse chunk = objectMapper.readValue(json, GenerateResponse.class);
                if (chunk.candidates() == null || chunk.candidates().isEmpty()) continue;
                Candidate candidate = chunk.candidates().getFirst();

                if (candidate.finishReason() != null) {
                    lastFinishReason[0] = candidate.finishReason();
                }

                String text = deltaText(candidate);
                if (text != null && !text.isEmpty()) {
                    emittedAny[0] = true;
                    onToken.accept(text);
                }
            }
        } catch (IOException e) {
            throw new AiProviderException("Failed reading Gemini stream", e);
        }

        // Mirrors extractText()'s guards below, but after the fact: a stream
        // can end having produced nothing (safety block, empty candidate)
        // with no single response object to inspect for the reason.
        if (!emittedAny[0]) {
            onToken.accept(NO_ANSWER_FALLBACK);
        } else if (MAX_TOKENS_REACHED.equals(lastFinishReason[0])) {
            log.warn("Gemini stream hit maxOutputTokens ({})", props.maxOutputTokens());
            onToken.accept("\n\n_(Response truncated — try a more specific question.)_");
        }
    }

    private String deltaText(Candidate candidate) {
        if (SAFETY_BLOCKED.equals(candidate.finishReason())) {
            log.warn("Gemini blocked a response chunk on safety grounds");
            return null;
        }
        if (candidate.content() == null
                || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            return null;
        }
        return candidate.content().parts().getFirst().text();
    }

    private String extractText(GenerateResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new AiProviderException("Gemini returned no candidates", null);
        }
        Candidate candidate = response.candidates().getFirst();

        // A safety-blocked candidate arrives with no content at all; without this
        // branch the checks below would NPE and look like an application bug.
        if (SAFETY_BLOCKED.equals(candidate.finishReason())) {
            log.warn("Gemini blocked a response on safety grounds");
            return NO_ANSWER_FALLBACK;
        }

        if (candidate.content() == null
                || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            throw new AiProviderException("Gemini returned an empty answer", null);
        }

        String text = candidate.content().parts().getFirst().text();
        if (text == null || text.isBlank()) {
            throw new AiProviderException("Gemini returned blank answer text", null);
        }

        // Truncated mid-sentence: better to say so than to serve a cut-off answer.
        if (MAX_TOKENS_REACHED.equals(candidate.finishReason())) {
            log.warn("Gemini response hit maxOutputTokens ({})", props.maxOutputTokens());
            return text + "\n\n_(Response truncated — try a more specific question.)_";
        }

        return text;
    }

    private record GenerateResponse(List<Candidate> candidates) {}

    private record Candidate(Content content, String finishReason) {}

    private record Content(List<Part> parts) {}

    private record Part(String text) {}
}
