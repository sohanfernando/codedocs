package com.sohan.codedocs.dto.response;

import com.sohan.codedocs.entity.ChatThread;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatThreadResponse(
        UUID id,

        /** The repo this thread was created from — used for sidebar nesting and as the single-repo default. */
        UUID repoId,

        /** Every repo this thread actually searches — one entry for an ordinary single-repo thread. */
        List<UUID> repoIds,

        String title,

        /** Null unless a public share link is currently active for this thread. */
        String shareToken,

        Instant createdAt,
        Instant updatedAt
) {
    public static ChatThreadResponse from(ChatThread thread, List<UUID> repoIds) {
        return new ChatThreadResponse(thread.getId(), thread.getRepoId(), repoIds, thread.getTitle(),
                thread.getShareToken(), thread.getCreatedAt(), thread.getUpdatedAt());
    }
}
