package com.sohan.codedocs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameThreadRequest(
        @NotBlank(message = "title is required")
        @Size(max = 255)
        String title
) {
    public RenameThreadRequest {
        title = title == null ? null : title.trim();
    }
}
