package com.sohan.codedocs.dto.response;

import com.sohan.codedocs.enums.ChatRole;
import com.sohan.codedocs.enums.Feedback;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        ChatRole role,
        String content,
        List<SourceRef> sources,
        boolean failed,
        Feedback feedback,
        Instant createdAt
) {
    public ChatMessageResponse {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
