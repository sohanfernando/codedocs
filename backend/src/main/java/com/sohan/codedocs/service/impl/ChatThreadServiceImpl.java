package com.sohan.codedocs.service.impl;

import com.sohan.codedocs.dto.response.ChatMessageResponse;
import com.sohan.codedocs.dto.response.ChatThreadResponse;
import com.sohan.codedocs.dto.response.SharedThreadResponse;
import com.sohan.codedocs.dto.response.SourceRef;
import com.sohan.codedocs.entity.ChatMessage;
import com.sohan.codedocs.entity.ChatThread;
import com.sohan.codedocs.entity.ChatThreadRepo;
import com.sohan.codedocs.enums.ChatRole;
import com.sohan.codedocs.enums.Feedback;
import com.sohan.codedocs.exception.ApiException;
import com.sohan.codedocs.exception.NotFoundException;
import com.sohan.codedocs.repository.ChatMessageRepository;
import com.sohan.codedocs.repository.ChatThreadRepoRepository;
import com.sohan.codedocs.repository.ChatThreadRepository;
import com.sohan.codedocs.repository.IndexedRepoRepository;
import com.sohan.codedocs.service.ChatThreadService;
import com.sohan.codedocs.service.RepoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatThreadServiceImpl implements ChatThreadService {

    private static final int TITLE_MAX_LENGTH = 80;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ChatThreadRepository chatThreadRepository;
    private final ChatThreadRepoRepository chatThreadRepoRepository;
    private final ChatMessageRepository chatMessageRepository;
    // Direct repository access, not RepoService: findByShareToken is the one
    // intentionally-public path in the app, so it must not be routed through
    // anything that carries an ownership check.
    private final IndexedRepoRepository indexedRepoRepository;
    private final RepoService repoService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ChatThreadResponse create(List<UUID> repoIds, UUID ownerId) {
        if (repoIds == null || repoIds.isEmpty()) {
            throw new ApiException("At least one repository is required.");
        }
        List<UUID> distinctRepoIds = repoIds.stream().distinct().toList();
        // 404s if any repo isn't the caller's — checked before anything is written.
        distinctRepoIds.forEach(repoId -> repoService.findOrThrow(repoId, ownerId));

        ChatThread thread = new ChatThread();
        thread.setRepoId(distinctRepoIds.get(0));
        thread.setOwnerId(ownerId);
        Instant now = Instant.now();
        thread.setCreatedAt(now);
        thread.setUpdatedAt(now);
        ChatThread saved = chatThreadRepository.save(thread);

        for (UUID repoId : distinctRepoIds) {
            ChatThreadRepo link = new ChatThreadRepo();
            link.setThreadId(saved.getId());
            link.setRepoId(repoId);
            chatThreadRepoRepository.save(link);
        }

        return ChatThreadResponse.from(saved, distinctRepoIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatThreadResponse> list(UUID repoId, UUID ownerId) {
        return chatThreadRepository.findAllByRepoIdAndOwnerIdOrderByUpdatedAtDesc(repoId, ownerId).stream()
                .map(thread -> ChatThreadResponse.from(thread, repoIdsFor(thread.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatThread findOrThrow(UUID id, UUID ownerId) {
        return chatThreadRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> repoIds(UUID threadId, UUID ownerId) {
        findOrThrow(threadId, ownerId); // ownership check
        return repoIdsFor(threadId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> messages(UUID threadId, UUID ownerId) {
        findOrThrow(threadId, ownerId); // ownership check before exposing anything
        return chatMessageRepository.findAllByThreadIdOrderByCreatedAtAsc(threadId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ChatThreadResponse rename(UUID id, UUID ownerId, String title) {
        ChatThread thread = findOrThrow(id, ownerId);
        thread.setTitle(title);
        ChatThread saved = chatThreadRepository.save(thread);
        return ChatThreadResponse.from(saved, repoIdsFor(id));
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID ownerId) {
        ChatThread thread = findOrThrow(id, ownerId);
        chatThreadRepository.delete(thread); // messages + chat_thread_repos cascade via ON DELETE CASCADE
    }

    @Override
    @Transactional
    public ChatThreadResponse share(UUID id, UUID ownerId) {
        ChatThread thread = findOrThrow(id, ownerId);
        if (thread.getShareToken() == null) {
            thread.setShareToken(generateShareToken());
            thread = chatThreadRepository.save(thread);
        }
        return ChatThreadResponse.from(thread, repoIdsFor(id));
    }

    @Override
    @Transactional
    public ChatThreadResponse unshare(UUID id, UUID ownerId) {
        ChatThread thread = findOrThrow(id, ownerId);
        thread.setShareToken(null);
        ChatThread saved = chatThreadRepository.save(thread);
        return ChatThreadResponse.from(saved, repoIdsFor(id));
    }

    @Override
    @Transactional(readOnly = true)
    public SharedThreadResponse findByShareToken(String token) {
        ChatThread thread = chatThreadRepository.findByShareToken(token)
                .orElseThrow(() -> new NotFoundException("Shared conversation not found"));

        // Not repoService.findOrThrow — a viewer here has no owner to check
        // against, and repo details are just display context, not access control.
        List<SharedThreadResponse.RepoSummary> repos = indexedRepoRepository
                .findAllById(repoIdsFor(thread.getId())).stream()
                .map(r -> new SharedThreadResponse.RepoSummary(r.getName(), r.getRemoteUrl()))
                .toList();

        List<ChatMessageResponse> messages = chatMessageRepository
                .findAllByThreadIdOrderByCreatedAtAsc(thread.getId()).stream()
                .map(this::toResponse)
                .toList();

        return new SharedThreadResponse(thread.getTitle(), repos, messages, thread.getCreatedAt());
    }

    private List<UUID> repoIdsFor(UUID threadId) {
        return chatThreadRepoRepository.findAllByThreadId(threadId).stream()
                .map(ChatThreadRepo::getRepoId)
                .toList();
    }

    /** 128 bits of randomness, URL-safe — long enough that guessing one is not a viable attack. */
    private String generateShareToken() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Its own transaction, separate from whatever's calling it (see
     * RagServiceImpl.answerStream): this must commit immediately regardless
     * of how long the Gemini call that follows takes.
     */
    @Override
    @Transactional
    public void appendUserMessage(UUID threadId, String content) {
        ChatThread thread = chatThreadRepository.findById(threadId).orElseThrow();

        // First message in the thread — title it from the question, the
        // same way a browser tab titles itself from a page's <h1>.
        if (thread.getTitle() == null) {
            thread.setTitle(truncateTitle(content));
        }
        thread.setUpdatedAt(Instant.now());
        chatThreadRepository.save(thread);

        ChatMessage message = new ChatMessage();
        message.setThreadId(threadId);
        message.setRole(ChatRole.USER);
        message.setContent(content);
        message.setCreatedAt(Instant.now());
        chatMessageRepository.save(message);
    }

    @Override
    @Transactional
    public UUID appendAssistantMessage(UUID threadId, String content, List<SourceRef> sources, boolean failed) {
        ChatThread thread = chatThreadRepository.findById(threadId).orElseThrow();
        thread.setUpdatedAt(Instant.now());
        chatThreadRepository.save(thread);

        ChatMessage message = new ChatMessage();
        message.setThreadId(threadId);
        message.setRole(ChatRole.ASSISTANT);
        message.setContent(content);
        message.setSourcesJson(toJson(sources));
        message.setFailed(failed);
        message.setCreatedAt(Instant.now());
        return chatMessageRepository.save(message).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> recentMessages(UUID threadId, int limit) {
        List<ChatMessage> recent = chatMessageRepository
                .findAllByThreadIdOrderByCreatedAtDesc(threadId, PageRequest.of(0, limit));
        Collections.reverse(recent); // back to chronological order
        return recent.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ChatMessageResponse setFeedback(UUID threadId, UUID messageId, UUID ownerId, Feedback vote) {
        findOrThrow(threadId, ownerId); // ownership check before touching anything
        ChatMessage message = chatMessageRepository.findByIdAndThreadId(messageId, threadId)
                .orElseThrow(() -> new NotFoundException("Message not found"));
        if (message.getRole() != ChatRole.ASSISTANT) {
            throw new ApiException("Feedback only applies to assistant answers.");
        }
        message.setFeedback(vote);
        return toResponse(chatMessageRepository.save(message));
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getRole(), message.getContent(),
                fromJson(message.getSourcesJson()), message.isFailed(), message.getFeedback(), message.getCreatedAt());
    }

    private String toJson(List<SourceRef> sources) {
        if (sources == null || sources.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (Exception e) {
            // Losing citations is much better than losing the answer itself.
            log.warn("Failed to serialize sources for a persisted chat message", e);
            return null;
        }
    }

    private List<SourceRef> fromJson(String json) {
        if (json == null) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<SourceRef>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize sources for a persisted chat message", e);
            return List.of();
        }
    }

    private String truncateTitle(String question) {
        String trimmed = question.trim();
        return trimmed.length() <= TITLE_MAX_LENGTH
                ? trimmed
                : trimmed.substring(0, TITLE_MAX_LENGTH - 1).stripTrailing() + "…";
    }
}
