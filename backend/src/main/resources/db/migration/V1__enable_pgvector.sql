-- Requires the pgvector/pgvector image (or the extension installed on the host).
-- The stock postgres:16 image does NOT ship this extension and will fail here.
CREATE EXTENSION IF NOT EXISTS vector;