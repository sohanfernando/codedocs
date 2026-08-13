package com.sohan.codedocs.service.impl;

import com.sohan.codedocs.config.properties.GeminiProperties;
import com.sohan.codedocs.enums.EmbeddingTaskType;
import com.sohan.codedocs.exception.AiProviderException;
import com.sohan.codedocs.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Isolated so @Retryable applies per single API call.
 * A retry annotation on the enclosing loop would restart the entire batch
 * after one rate-limited call — and self-invocation would bypass the proxy
 * entirely, silently disabling retries.
 */
@Slf4j
@Component
public class GeminiApiCaller {

    private final RestClient restClient;
    private final GeminiProperties props;

    public GeminiApiCaller(@Qualifier("geminiRestClient") RestClient restClient,
                           GeminiProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    @Retryable(
            includes = {RateLimitException.class, ResourceAccessException.class},
            maxRetries = 4,
            delayString = "2s",
            multiplier = 2,
            maxDelay = 30_000
    )
    public List<Double> embedOne(String text, EmbeddingTaskType taskType) {
        Map<String, Object> body = Map.of(
                "model", "models/" + props.embeddingModel(),
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "taskType", taskType.name(),
                "outputDimensionality", props.embeddingDimensions());

        EmbedResponse response = restClient.post()
                .uri("/v1beta/models/{model}:embedContent", props.embeddingModel())
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    if (res.getStatusCode().value() == 429) {
                        throw new RateLimitException("Gemini embedding quota exceeded");
                    }
                    String error = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    log.error("Gemini embedding error {}: {}", res.getStatusCode(), error);
                    throw new AiProviderException(
                            "Gemini rejected the embedding request: " + res.getStatusCode(), null);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) ->
                { throw new RateLimitException("Gemini unavailable: " + res.getStatusCode()); })
                .body(EmbedResponse.class);

        if (response == null || response.embedding() == null || response.embedding().values() == null) {
            throw new AiProviderException("Empty embedding response from Gemini", null);
        }
        return response.embedding().values();
    }

    private record EmbedResponse(Embedding embedding) {}

    private record Embedding(List<Double> values) {}
}
