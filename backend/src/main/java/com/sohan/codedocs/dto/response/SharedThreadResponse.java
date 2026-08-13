package com.sohan.codedocs.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * The public view of a shared thread. Deliberately narrow: no owner id, no
 * email, no internal thread id — just what a link recipient should see.
 */
public record SharedThreadResponse(
        String title,
        List<RepoSummary> repos,
        List<ChatMessageResponse> messages,
        Instant createdAt
) {
    public record RepoSummary(String name, String url) {}
}
