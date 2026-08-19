package com.sohan.codedocs.config;

import com.sohan.codedocs.dto.response.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int INGEST_CAPACITY = 5;     // repos per hour per IP
    private static final int CHAT_CAPACITY = 30;      // questions per minute per IP
    private static final int LOGIN_CAPACITY = 10;     // attempts per 15 minutes per IP
    private static final int REGISTER_CAPACITY = 5;   // registrations per hour per IP

    private final Map<String, Bucket> ingestBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> chatBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!"POST".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        Bucket bucket = null;
        if (path.startsWith("/api/repositories")) {
            bucket = ingestBuckets.computeIfAbsent(clientKey(request),
                    k -> newBucket(INGEST_CAPACITY, Duration.ofHours(1)));
        } else if (path.startsWith("/api/chat")) {
            bucket = chatBuckets.computeIfAbsent(clientKey(request),
                    k -> newBucket(CHAT_CAPACITY, Duration.ofMinutes(1)));
        } else if (path.equals("/api/auth/login")) {
            bucket = loginBuckets.computeIfAbsent(clientKey(request),
                    k -> newBucket(LOGIN_CAPACITY, Duration.ofMinutes(15)));
        } else if (path.equals("/api/auth/register")) {
            bucket = registerBuckets.computeIfAbsent(clientKey(request),
                    k -> newBucket(REGISTER_CAPACITY, Duration.ofHours(1)));
        }

        if (bucket != null && !bucket.tryConsume(1)) {
            writeTooManyRequests(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private Bucket newBucket(int capacity, Duration refill) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(capacity, refill)
                        .build())
                .build();
    }

    private String clientKey(HttpServletRequest request) {
        // Only trust X-Forwarded-For if a reverse proxy you control sets it.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                new ErrorResponse("RATE_LIMITED", "Too many requests. Please slow down."));
    }
}