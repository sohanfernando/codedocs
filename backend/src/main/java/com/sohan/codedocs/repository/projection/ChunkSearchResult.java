package com.sohan.codedocs.repository.projection;

import java.util.UUID;

public interface ChunkSearchResult {
    String getContent();
    int getStartLine();
    int getEndLine();
    String getFilePath();
    String getLanguage();
    double getSimilarity();

    /** Which repo this chunk came from — a search can now span more than one. */
    UUID getRepoId();
}
