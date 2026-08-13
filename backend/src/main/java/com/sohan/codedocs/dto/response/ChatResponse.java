package com.sohan.codedocs.dto.response;

import java.util.List;

public record ChatResponse(String answer, List<SourceRef> sources) {

    public ChatResponse {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

    public static ChatResponse noContext() {
        return new ChatResponse(
                "I couldn't find anything relevant to that question in this repository.",
                List.of());
    }
}