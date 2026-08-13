package com.sohan.codedocs.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "ingestion")
public record IngestionProperties(
        @Min(1) int maxFilesPerRepo,
        @Min(1024) long maxFileBytes,
        @Min(1024) long maxTotalBytes,
        @Min(10) int chunkWindowLines,
        @Min(0) int chunkOverlapLines,
        @Min(1) int chunkMaxChars,
        Duration cloneTimeout
) {}