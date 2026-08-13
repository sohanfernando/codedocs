package com.sohan.codedocs.dto.response;

public record SourceRef(
        int index,
        String filePath,
        int startLine,
        int endLine,
        String githubUrl,
        double similarity
) {}
