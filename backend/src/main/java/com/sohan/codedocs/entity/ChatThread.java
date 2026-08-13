package com.sohan.codedocs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "chat_threads", indexes = @Index(name = "idx_threads_repo_owner", columnList = "repo_id, owner_id"))
public class ChatThread extends BaseEntity {

    // Plain FKs, not @ManyToOne — same reasoning as IndexedRepo.ownerId:
    // every access pattern here is an id comparison or a WHERE clause,
    // never a navigation, so there's no reason to take on a lazy association.
    @Column(name = "repo_id", nullable = false)
    private UUID repoId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    /** Null until the first message lands; see ChatThreadServiceImpl.appendUserMessage. */
    @Column(length = 255)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Null unless this thread has an active public share link. See ChatThreadServiceImpl.share/unshare. */
    @Column(name = "share_token", length = 32)
    private String shareToken;
}
