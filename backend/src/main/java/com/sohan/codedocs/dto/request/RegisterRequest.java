package com.sohan.codedocs.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "email is required")
        @Email(message = "must be a valid email address")
        @Size(max = 255)
        String email,

        // BCrypt silently ignores bytes past 72 — capping here means the
        // password a user types is actually the password that gets checked.
        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8-72 characters")
        String password
) {
    public RegisterRequest {
        email = email == null ? null : email.trim();
    }
}
