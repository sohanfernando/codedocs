package com.sohan.codedocs.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse (
    String code,
    String message,
    List<String> details,
    Instant timestamp
) {
    public ErrorResponse(String code, String message){
        this(code, message, null, Instant.now());
    }

    public ErrorResponse(String code, String message, List<String> details) {
        this(code, message, details, Instant.now());
    }
}
