package com.sohan.codedocs.dto.response;

import com.sohan.codedocs.entity.IndexedRepo;
import com.sohan.codedocs.enums.RepoStatus;

import java.time.Instant;
import java.util.UUID;

public record RepoResponse(
        UUID id,
        String name,
        String remoteUrl,
        String branch,
        RepoStatus status,
        int documentCount,
        int chunkCount,
        String errorMessage,
        Instant indexedAt
) {
    public static RepoResponse from(IndexedRepo repo) {
        return new RepoResponse(
                repo.getId(), repo.getName(), repo.getRemoteUrl(), repo.getBranch(),
                repo.getStatus(), repo.getDocumentCount(), repo.getChunkCount(),
                repo.getErrorMessage(), repo.getIndexedAt());
    }
}
