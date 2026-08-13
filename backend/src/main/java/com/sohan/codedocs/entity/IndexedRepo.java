package com.sohan.codedocs.entity;

import com.sohan.codedocs.enums.RepoStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "indexed_repos", indexes = @Index(name = "idx_repos_status", columnList = "status"))
public class IndexedRepo extends BaseEntity{

    /**
     * Plain FK, not a @ManyToOne to User: nothing here ever needs to
     * navigate to the owner's other fields, just compare an id, so there's
     * no reason to take on a lazy association (and the loading concerns
     * that come with one — see RagServiceImpl.retrieve()).
     */
    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "remote_url", nullable = false, length = 512)
    private String remoteUrl;

    @Column(length = 255)
    private String name;

    @Column(length = 255)
    private String branch;

    //** Pins source links so they survive force-pushes. */
    @Column(name = "commit_sha", length = 40)
    private String commitSha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RepoStatus status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "document_count", nullable = false)
    private int documentCount;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    /** Guards against serving results from vectors built by a different model. */
    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "embedding_dimensions")
    private Integer embeddingDimensions;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "indexed_at")
    private Instant indexedAt;

    @Version
    private Long version;
}
