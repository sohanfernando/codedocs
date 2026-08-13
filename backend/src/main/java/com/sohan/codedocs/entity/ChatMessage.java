package com.sohan.codedocs.entity;

import com.sohan.codedocs.enums.ChatRole;
import com.sohan.codedocs.enums.Feedback;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "chat_messages", indexes = @Index(name = "idx_messages_thread", columnList = "thread_id, created_at"))
public class ChatMessage extends BaseEntity {

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChatRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** JSON-serialized List<SourceRef>, frozen at answer time — see the V6 migration comment. */
    @Column(name = "sources_json", columnDefinition = "text")
    private String sourcesJson;

    @Column(nullable = false)
    private boolean failed;

    /** Null means no vote. Only meaningful on ASSISTANT messages. */
    @Enumerated(EnumType.STRING)
    @Column(length = 4)
    private Feedback feedback;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
