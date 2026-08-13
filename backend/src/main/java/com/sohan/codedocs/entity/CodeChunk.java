package com.sohan.codedocs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "code_chunks",
        indexes = @Index(name = "idx_chunks_document", columnList = "document_id"))
public class CodeChunk extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private SourceDocument document;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "start_line", nullable = false)
    private int startLine;

    @Column(name = "end_line", nullable = false)
    private int endLine;

    @Column(name = "token_count")
    private Integer tokenCount;

    /**
     * Dimension must stay in sync across three places: this annotation,
     * V2__create_tables.sql, and gemini.embedding-dimensions.
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    @Column(name = "embedding")
    private float[] embedding;
}
