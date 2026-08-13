package com.sohan.codedocs.repository;

import com.sohan.codedocs.entity.CodeChunk;
import com.sohan.codedocs.repository.projection.ChunkSearchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CodeChunkRepository extends JpaRepository<CodeChunk, UUID> {

    /**
     * Cosine similarity search, scoped to one or more repositories (a
     * conversation can span several) — a single combined ANN query and
     * LIMIT across all of them, not top-K per repo then merged.
     *
     * ORDER BY uses the raw distance operator. Wrapping it in arithmetic
     * (e.g. ORDER BY 1 - (...)) prevents the HNSW index from being used and
     * silently degrades to a sequential scan over every chunk.
     *
     * The query vector is bound as a parameter and cast, never concatenated —
     * concatenation here would be a SQL injection vector.
     */
    @Query(value = """
            SELECT c.content       AS content,
                   c.start_line    AS startLine,
                   c.end_line      AS endLine,
                   d.file_path     AS filePath,
                   d.language      AS language,
                   d.repo_id       AS repoId,
                   1 - (c.embedding <=> CAST(:queryVector AS vector)) AS similarity
            FROM code_chunks c
            JOIN source_documents d ON d.id = c.document_id
            WHERE d.repo_id IN (:repoIds)
              AND c.embedding IS NOT NULL
            ORDER BY c.embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<ChunkSearchResult> searchByEmbedding(@Param("repoIds") List<UUID> repoIds,
                                              @Param("queryVector") String queryVector,
                                              @Param("topK") int topK);

    /** Recount after an incremental sync rather than tracking a running total through the diff. */
    long countByDocumentRepoId(UUID repoId);
}
