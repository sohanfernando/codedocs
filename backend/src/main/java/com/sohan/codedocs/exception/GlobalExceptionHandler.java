package com.sohan.codedocs.exception;

import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.sohan.codedocs.dto.response.ErrorResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    /**
     * Covers AuthController's login failures (bad credentials, unknown
     * email) — deliberately the same message either way, so the response
     * never confirms whether an email address has an account.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password."));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", "Request validation failed", details));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleMalformed(Exception ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("MALFORMED_REQUEST", "Request body or parameter is invalid"));
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse("RATE_LIMITED",
                        "AI provider rate limit reached. Please try again shortly."));
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ErrorResponse> handleAiProvider(AiProviderException ex) {
        log.error("AI provider failure", ex);
        Sentry.captureException(ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("AI_PROVIDER_ERROR",
                        "The AI service is currently unavailable."));
    }

    /**
     * Catch-all for genuinely unexpected failures.
     *
     * RuntimeException, not Exception: catching Exception makes this handler a
     * candidate for every custom exception above it and it can win the match,
     * turning a deliberate 400 into a 500.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(RuntimeException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unhandled exception [errorId={}]", errorId, ex);
        // Tagged with the same errorId a caller can quote back to us, so a
        // support conversation ("reference: xyz") jumps straight to the
        // matching Sentry event instead of a log grep.
        Sentry.captureException(ex, scope -> scope.setTag("errorId", errorId));
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR",
                        "An unexpected error occurred. Reference: " + errorId));
    }
}
