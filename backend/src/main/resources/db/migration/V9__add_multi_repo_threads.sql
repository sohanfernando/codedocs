-- ---------------------------------------------------------------------------
-- A thread can now span more than one repo — this is the actual set that
-- gets searched. chat_threads.repo_id is kept as the "primary" repo (used
-- for sidebar nesting and as a sane single-repo default); this table is the
-- source of truth for what actually gets queried.
-- ---------------------------------------------------------------------------
CREATE TABLE chat_thread_repos (
                                    id        UUID PRIMARY KEY,
                                    thread_id UUID NOT NULL REFERENCES chat_threads (id) ON DELETE CASCADE,
                                    repo_id   UUID NOT NULL REFERENCES indexed_repos (id) ON DELETE CASCADE,

                                    UNIQUE (thread_id, repo_id)
);

CREATE INDEX idx_thread_repos_thread ON chat_thread_repos (thread_id);
CREATE INDEX idx_thread_repos_repo ON chat_thread_repos (repo_id);

-- Backfill: every existing thread already has exactly one repo — its
-- current repo_id column — so that's its initial (and so far only) entry here.
INSERT INTO chat_thread_repos (id, thread_id, repo_id)
SELECT gen_random_uuid(), id, repo_id FROM chat_threads;
