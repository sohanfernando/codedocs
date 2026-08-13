-- ---------------------------------------------------------------------------
-- Public, read-only sharing. A thread with a non-null share_token is
-- viewable by anyone with the link, no account required — the one
-- deliberately unauthenticated read path in the whole API.
--
-- A fresh random token, not the thread's own id: decouples the public URL
-- from the internal primary key (no reason to expose whether ids are
-- sequential or guessable) and lets a link be revoked and re-created
-- without touching the thread's identity.
-- ---------------------------------------------------------------------------
ALTER TABLE chat_threads
    ADD COLUMN share_token VARCHAR(32);

-- Partial index: uniqueness only matters among rows that are actually
-- shared, and most rows will have a null token.
CREATE UNIQUE INDEX uq_threads_share_token ON chat_threads (share_token)
    WHERE share_token IS NOT NULL;
