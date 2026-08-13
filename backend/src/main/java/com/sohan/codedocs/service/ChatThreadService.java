package com.sohan.codedocs.service;

import com.sohan.codedocs.dto.response.ChatMessageResponse;
import com.sohan.codedocs.dto.response.ChatThreadResponse;
import com.sohan.codedocs.dto.response.SharedThreadResponse;
import com.sohan.codedocs.dto.response.SourceRef;
import com.sohan.codedocs.entity.ChatThread;
import com.sohan.codedocs.enums.Feedback;

import java.util.List;
import java.util.UUID;

public interface ChatThreadService {
    ChatThreadResponse create(List<UUID> repoIds, UUID ownerId);

    List<ChatThreadResponse> list(UUID repoId, UUID ownerId);

    /** The entity, not a response — for RagServiceImpl, which needs repoId etc., not a DTO to hand back over HTTP. */
    ChatThread findOrThrow(UUID id, UUID ownerId);

    /** Every repo this thread searches, ownership-checked. */
    List<UUID> repoIds(UUID threadId, UUID ownerId);

    List<ChatMessageResponse> messages(UUID threadId, UUID ownerId);

    ChatThreadResponse rename(UUID id, UUID ownerId, String title);

    void delete(UUID id, UUID ownerId);

    /** Idempotent: returns the existing link if one's already active rather than rotating it. */
    ChatThreadResponse share(UUID id, UUID ownerId);

    ChatThreadResponse unshare(UUID id, UUID ownerId);

    /** The public, unauthenticated lookup — deliberately no ownerId parameter. */
    SharedThreadResponse findByShareToken(String token);

    /** Titles the thread from this question if it's the first message. */
    void appendUserMessage(UUID threadId, String content);

    /** Returns the persisted message's id — see ChatStreamListener.onComplete for why the caller needs it. */
    UUID appendAssistantMessage(UUID threadId, String content, List<SourceRef> sources, boolean failed);

    /** The last `limit` messages, oldest first — feeds multi-turn context into the next answer. */
    List<ChatMessageResponse> recentMessages(UUID threadId, int limit);

    /** vote == null clears any existing feedback. Only valid on an ASSISTANT message. */
    ChatMessageResponse setFeedback(UUID threadId, UUID messageId, UUID ownerId, Feedback vote);
}
