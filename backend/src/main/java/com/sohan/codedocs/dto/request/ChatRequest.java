package com.sohan.codedocs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatRequest(

        @NotNull(message = "threadId is required")
        UUID threadId,

        @NotBlank(message = "question is required")
        @Size(max = 1000, message = "question must be 1000 characters or fewer")
        String question
) {
    public ChatRequest {
        question = question == null ? null : question.trim();
    }
}
