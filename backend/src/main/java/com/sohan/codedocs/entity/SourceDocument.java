package com.sohan.codedocs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "source_documents",
        indexes = @Index(name = "idx_documents_repo", columnList = "repo_id"))
public class SourceDocument extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repo_id", nullable = false,
                foreignKey = @jakarta.persistence.ForeignKey(name = "fk_documents_repo"))
    private IndexedRepo repo;

    /** Relative to the repo root — the temp clone path must never be persisted. */
    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    @Column(length = 32)
    private String language;

    @Column(name = "content_sha", length = 64)
    private String contentSha;
}
