package com.sohan.codedocs.controller;

import com.sohan.codedocs.dto.response.SharedThreadResponse;
import com.sohan.codedocs.service.ChatThreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The one deliberately unauthenticated read path in the API — see
 * SecurityConfig's permitAll rule for this path and ChatThreadService's
 * findByShareToken javadoc.
 */
@RestController
@RequestMapping("/api/shared")
@RequiredArgsConstructor
public class SharedThreadController {

    private final ChatThreadService chatThreadService;

    @GetMapping("/{token}")
    public SharedThreadResponse get(@PathVariable String token) {
        return chatThreadService.findByShareToken(token);
    }
}
