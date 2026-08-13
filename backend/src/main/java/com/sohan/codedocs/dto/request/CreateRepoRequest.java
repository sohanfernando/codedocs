package com.sohan.codedocs.dto.request;

import com.sohan.codedocs.validation.ValidGitHubUrl;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRepoRequest (

    @ValidGitHubUrl
    String gitUrl,

    @Size(max = 255)
    @Pattern(regexp = "^[A-Za-z0-9._/-]*$", message = "invalid branch name")
    String branch
) {
    public CreateRepoRequest {
        gitUrl = gitUrl == null ? null : gitUrl.trim();
        branch = (branch == null || branch.isBlank()) ? null : branch.trim();
    }
}
