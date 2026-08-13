package com.sohan.codedocs.ingestion.model;

import java.util.List;

public record ScannedFile(String relativePath, String extension,
                          String language, List<String> lines) {
    public ScannedFile {
        lines = List.copyOf(lines);
    }
}
