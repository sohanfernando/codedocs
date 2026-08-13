package com.sohan.codedocs.repository;

import com.sohan.codedocs.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findAllByThreadIdOrderByCreatedAtAsc(UUID threadId);

    /** Most-recent-first, capped by the Pageable — used to build multi-turn context. */
    List<ChatMessage> findAllByThreadIdOrderByCreatedAtDesc(UUID threadId, Pageable pageable);

    Optional<ChatMessage> findByIdAndThreadId(UUID id, UUID threadId);
}
