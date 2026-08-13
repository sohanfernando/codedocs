package com.sohan.codedocs.service;

import com.sohan.codedocs.dto.response.SourceRef;

import java.util.List;
import java.util.UUID;

/**
 * Streaming callback for {@link RagService#answerStream}.
 *
 * Sources arrive once, up front — as soon as retrieval finishes, before any
 * model call — so the UI can render citations while the answer text is
 * still being generated. Exactly one of onComplete/onError follows, always
 * last.
 */
public interface ChatStreamListener {
    void onSources(List<SourceRef> sources);

    void onToken(String token);

    /** messageId is the persisted assistant message's real id — the client's placeholder id is only a stand-in until now. */
    void onComplete(UUID messageId);

    void onError(Exception ex);
}
