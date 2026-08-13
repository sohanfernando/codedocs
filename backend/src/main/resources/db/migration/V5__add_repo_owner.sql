-- ---------------------------------------------------------------------------
-- Per-user repo scoping.
--
-- Nullable, not NOT NULL: this repo predates accounts, and a hard constraint
-- here would fail the migration outright on any database that already has
-- rows. New repos are always created with an owner (see RepoServiceImpl);
-- an unowned row simply becomes invisible to everyone, which is an
-- acceptable fate for pre-auth dev data.
-- ---------------------------------------------------------------------------
ALTER TABLE indexed_repos
    ADD COLUMN owner_id UUID REFERENCES users (id) ON DELETE CASCADE;

CREATE INDEX idx_repos_owner ON indexed_repos (owner_id);
