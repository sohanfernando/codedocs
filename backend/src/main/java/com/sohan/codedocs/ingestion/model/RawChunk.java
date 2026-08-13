package com.sohan.codedocs.ingestion.model;

public record RawChunk(String content, int startLine, int endLine) {
    public RawChunk {
        if (startLine < 1 || endLine < startLine) {
            throw new IllegalArgumentException(
                    "invalid line range: %d-%d".formatted(startLine, endLine)
            );
        }
    }
}
