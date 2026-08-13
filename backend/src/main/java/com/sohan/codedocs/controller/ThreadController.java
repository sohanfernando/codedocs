package com.sohan.codedocs.controller;

import com.sohan.codedocs.dto.request.CreateThreadRequest;
import com.sohan.codedocs.dto.request.FeedbackRequest;
import com.sohan.codedocs.dto.request.RenameThreadRequest;
import com.sohan.codedocs.dto.response.ChatMessageResponse;
import com.sohan.codedocs.dto.response.ChatThreadResponse;
import com.sohan.codedocs.security.AppUserDetails;
import com.sohan.codedocs.service.ChatThreadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/threads")
@RequiredArgsConstructor
public class ThreadController {

    private final ChatThreadService chatThreadService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatThreadResponse create(@Valid @RequestBody CreateThreadRequest request,
                                     @AuthenticationPrincipal AppUserDetails principal) {
        return chatThreadService.create(request.repoIds(), principal.id());
    }

    @GetMapping
    public List<ChatThreadResponse> list(@RequestParam UUID repoId,
                                         @AuthenticationPrincipal AppUserDetails principal) {
        return chatThreadService.list(repoId, principal.id());
    }

    @GetMapping("/{id}/messages")
    public List<ChatMessageResponse> messages(@PathVariable UUID id,
                                              @AuthenticationPrincipal AppUserDetails principal) {
        return chatThreadService.messages(id, principal.id());
    }

    @PatchMapping("/{id}")
    public ChatThreadResponse rename(@PathVariable UUID id, @Valid @RequestBody RenameThreadRequest request,
                                     @AuthenticationPrincipal AppUserDetails principal) {
        return chatThreadService.rename(id, principal.id(), request.title());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        chatThreadService.delete(id, principal.id());
    }

    @PostMapping("/{id}/share")
    public ChatThreadResponse share(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        return chatThreadService.share(id, principal.id());
    }

    @DeleteMapping("/{id}/share")
    public ChatThreadResponse unshare(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        return chatThreadService.unshare(id, principal.id());
    }

    @PutMapping("/{id}/messages/{messageId}/feedback")
    public ChatMessageResponse setFeedback(@PathVariable UUID id, @PathVariable UUID messageId,
                                           @RequestBody FeedbackRequest request,
                                           @AuthenticationPrincipal AppUserDetails principal) {
        return chatThreadService.setFeedback(id, messageId, principal.id(), request.vote());
    }
}
