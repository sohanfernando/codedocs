package com.sohan.codedocs.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "email is required")
        String email,

        @NotBlank(message = "password is required")
        String password
) {
    public LoginRequest {
        email = email == null ? null : email.trim();
    }
}
