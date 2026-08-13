package com.sohan.codedocs.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(

        @NotBlank(message = "GEMINI_API_KEY must be provided")
        String apiKey,

        @NotBlank String baseUrl,
        @NotBlank String embeddingModel,
        @NotBlank String chatModel,

        @Min(128) @Max(3072) int embeddingDimensions,
        @Min(1) @Max(100) int embeddingBatchSize,

        Duration requestTimeout,
        double temperature,
        int maxOutputTokens
) {
    /** Never let the key reach a log or an error response. */
    @Override
    public String toString() {
        return "GeminiProperties[apiKey=***, embeddingModel=%s, chatModel=%s, dims=%d]"
                .formatted(embeddingModel, chatModel, embeddingDimensions);
    }
}