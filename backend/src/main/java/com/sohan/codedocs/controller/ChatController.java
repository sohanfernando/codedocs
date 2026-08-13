package com.sohan.codedocs.controller;

import com.sohan.codedocs.config.AsyncConfig;
import com.sohan.codedocs.dto.request.ChatRequest;
import com.sohan.codedocs.dto.response.ChatResponse;
import com.sohan.codedocs.dto.response.SourceRef;
import com.sohan.codedocs.exception.AiProviderException;
import com.sohan.codedocs.exception.ApiException;
import com.sohan.codedocs.exception.NotFoundException;
import com.sohan.codedocs.exception.RateLimitException;
import com.sohan.codedocs.security.AppUserDetails;
import com.sohan.codedocs.service.ChatStreamListener;
import com.sohan.codedocs.service.RagService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    // Comfortably above GeminiProperties.requestTimeout (60s default) plus
    // generation time — this bounds the whole stream, not a single read.
    private static final long STREAM_TIMEOUT_MS = 180_000L;

    private final RagService ragService;
    private final Executor chatStreamExecutor;

    public ChatController(RagService ragService,
                          @Qualifier(AsyncConfig.CHAT_STREAM_EXECUTOR) Executor chatStreamExecutor) {
        this.ragService = ragService;
        this.chatStreamExecutor = chatStreamExecutor;
    }

    @PostMapping
    public ChatResponse ask(@Valid @RequestBody ChatRequest request,
                            @AuthenticationPrincipal AppUserDetails principal) {
        return ragService.answer(request, principal.id());
    }

    /**
     * SSE, not the native EventSource API: EventSource can only issue GET
     * requests, and the question needs to travel in a body — a free-text
     * question plus a UUID doesn't belong in a query string. The frontend
     * reads this with fetch() and a manual ReadableStream loop instead of
     * `new EventSource(...)`.
     *
     * The work happens on chatStreamExecutor, not the request thread: the
     * emitter is returned immediately and Spring MVC's async support keeps
     * the HTTP response open while ragService.answerStream runs in the
     * background and pushes events as they're produced.
     */
    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@Valid @RequestBody ChatRequest request,
                                @AuthenticationPrincipal AppUserDetails principal) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        UUID ownerId = principal.id();

        chatStreamExecutor.execute(() -> ragService.answerStream(request, ownerId, new ChatStreamListener() {
            @Override
            public void onSources(List<SourceRef> sources) {
                send(emitter, "sources", sources);
            }

            @Override
            public void onToken(String token) {
                send(emitter, "token", Map.of("text", token));
            }

            @Override
            public void onComplete(UUID messageId) {
                send(emitter, "done", Map.of("messageId", messageId));
                emitter.complete();
            }

            @Override
            public void onError(Exception ex) {
                if (ex instanceof AiProviderException) {
                    log.error("Chat stream failed", ex);
                } else {
                    log.warn("Chat stream failed: {}", ex.getMessage());
                }
                send(emitter, "error", Map.of("code", errorCode(ex), "message", errorMessage(ex)));
                emitter.complete();
            }
        }));

        return emitter;
    }

    /** A failed send means the client is gone — nothing left to notify. */
    private void send(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException ex) {
            emitter.completeWithError(ex);
        }
    }

    // Mirrors GlobalExceptionHandler's code strings — that advice can't run
    // here since the SSE response is already committed with a 200 by the
    // time any of this fires.
    private String errorCode(Exception ex) {
        if (ex instanceof NotFoundException) return "NOT_FOUND";
        if (ex instanceof ApiException) return "BAD_REQUEST";
        if (ex instanceof RateLimitException) return "RATE_LIMITED";
        if (ex instanceof AiProviderException) return "AI_PROVIDER_ERROR";
        return "INTERNAL_ERROR";
    }

    private String errorMessage(Exception ex) {
        if (ex instanceof RateLimitException) {
            return "AI provider rate limit reached. Please try again shortly.";
        }
        if (ex instanceof AiProviderException) {
            return "The AI service is currently unavailable.";
        }
        if (ex instanceof NotFoundException || ex instanceof ApiException) {
            return ex.getMessage();
        }
        return "An unexpected error occurred.";
    }
}
