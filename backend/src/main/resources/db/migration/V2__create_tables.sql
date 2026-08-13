-- ---------------------------------------------------------------------------
-- Repositories submitted for indexing.
-- ---------------------------------------------------------------------------
CREATE TABLE indexed_repos (
                               id                   UUID         PRIMARY KEY,
                               remote_url           VARCHAR(512) NOT NULL,
                               name                 VARCHAR(255),
                               branch               VARCHAR(255),

    -- Pins source links to an immutable commit so they survive force-pushes.
                               commit_sha           VARCHAR(40),

                               status               VARCHAR(32)  NOT NULL,
                               error_message        VARCHAR(500),

                               document_count       INTEGER      NOT NULL DEFAULT 0,
                               chunk_count          INTEGER      NOT NULL DEFAULT 0,

    -- Vectors are only comparable to a query embedded by the same model.
    -- Stored per repo so a model change is detectable instead of silently
    -- returning nonsense.
                               embedding_model      VARCHAR(100),
                               embedding_dimensions INTEGER,

                               created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
                               indexed_at           TIMESTAMPTZ,

    -- Backs @Version on the entity (optimistic locking).
                               version              BIGINT       NOT NULL DEFAULT 0,

                               CONSTRAINT chk_repos_status CHECK (
                                   status IN ('PENDING', 'CLONING', 'CHUNKING', 'EMBEDDING', 'READY', 'FAILED')
                                   )
);

-- ---------------------------------------------------------------------------
-- One row per indexed file.
-- ---------------------------------------------------------------------------
CREATE TABLE source_documents (
                                  id          UUID          PRIMARY KEY,
                                  repo_id     UUID          NOT NULL,

    -- Relative to the repo root. The temp clone path must never be persisted:
    -- it is deleted after ingestion and would produce dead links.
                                  file_path   VARCHAR(1024) NOT NULL,

                                  language    VARCHAR(32),
                                  content_sha VARCHAR(64),

                                  CONSTRAINT fk_documents_repo FOREIGN KEY (repo_id)
                                      REFERENCES indexed_repos (id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------------
-- Chunks: the unit of retrieval.
-- ---------------------------------------------------------------------------
CREATE TABLE code_chunks (
                             id          UUID    PRIMARY KEY,
                             document_id UUID    NOT NULL,
                             content     TEXT    NOT NULL,

    -- 1-indexed to match GitHub's #L40-L98 anchor format.
                             start_line  INTEGER NOT NULL,
                             end_line    INTEGER NOT NULL,
                             token_count INTEGER,

    -- Dimension must match @Array(length = 768) on CodeChunk
    -- and gemini.embedding-dimensions in application.yml.
                             embedding   VECTOR(768),

                             CONSTRAINT fk_chunks_document FOREIGN KEY (document_id)
                                 REFERENCES source_documents (id) ON DELETE CASCADE,

                             CONSTRAINT chk_chunks_lines CHECK (start_line >= 1 AND end_line >= start_line)
);

CREATE INDEX idx_repos_status    ON indexed_repos (status);
CREATE INDEX idx_documents_repo  ON source_documents (repo_id);
CREATE INDEX idx_chunks_document ON code_chunks (document_id);