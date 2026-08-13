-- ---------------------------------------------------------------------------
-- A named conversation, scoped to one repo and one owner. Multiple threads
-- per repo are normal — this is the ChatGPT-style "history" list.
-- ---------------------------------------------------------------------------
CREATE TABLE chat_threads (
                              id         UUID         PRIMARY KEY,
                              repo_id    UUID         NOT NULL REFERENCES indexed_repos (id) ON DELETE CASCADE,
                              owner_id   UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    -- Null until the first message lands; auto-derived from the first
    -- question (see ChatThreadServiceImpl), renameable after that.
                              title      VARCHAR(255),

                              created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Bumped on every new message. Threads list by this, most-recently-
    -- active first, same as every other chat app's sidebar.
                              updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_threads_repo_owner ON chat_threads (repo_id, owner_id);

-- ---------------------------------------------------------------------------
-- One row per message. Assistant messages freeze their sources as JSON at
-- write time rather than recomputing GitHub links later — a repo re-index
-- moves commit_sha forward, and recomputing would silently repoint old
-- history at a commit that was never actually current when the answer
-- was given.
-- ---------------------------------------------------------------------------
CREATE TABLE chat_messages (
                               id           UUID        PRIMARY KEY,
                               thread_id    UUID        NOT NULL REFERENCES chat_threads (id) ON DELETE CASCADE,

                               role         VARCHAR(16) NOT NULL,
                               content      TEXT        NOT NULL,

    -- JSON-serialized List<SourceRef>; null for user messages and for
    -- assistant messages with no relevant context.
                               sources_json TEXT,

    -- Mirrors the frontend's Message.failed — a resumed thread can still
    -- show which answers errored instead of presenting them as normal.
                               failed       BOOLEAN     NOT NULL DEFAULT false,

                               created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

                               CONSTRAINT chk_messages_role CHECK (role IN ('USER', 'ASSISTANT'))
);

CREATE INDEX idx_messages_thread ON chat_messages (thread_id, created_at);
