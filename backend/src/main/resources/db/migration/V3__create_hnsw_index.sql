-- Approximate nearest-neighbour index for the vector search.
--
-- vector_cosine_ops MUST match the operator used in CodeChunkRepository (<=>).
-- Pairing it with <-> (L2) silently disables the index: queries still return
-- correct results, just via a sequential scan over every chunk.
--
-- Separate migration on purpose: for a large bulk load it is far faster to
-- drop this index, insert, then recreate it than to insert through a live
-- HNSW index.
--
-- m = 16              connections per node (recall vs. index size)
-- ef_construction = 64  build-time candidate list (recall vs. build time)
CREATE INDEX idx_chunks_embedding
    ON code_chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);