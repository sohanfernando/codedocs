package com.sohan.codedocs.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record CreateThreadRequest(
        @NotEmpty(message = "At least one repository is required")
        List<UUID> repoIds
) {}
