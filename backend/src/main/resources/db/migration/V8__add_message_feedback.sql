-- ---------------------------------------------------------------------------
-- Thumbs up/down on an assistant answer. Null means no vote — most rows.
-- ---------------------------------------------------------------------------
ALTER TABLE chat_messages
    ADD COLUMN feedback VARCHAR(4);

ALTER TABLE chat_messages
    ADD CONSTRAINT chk_messages_feedback CHECK (feedback IN ('UP', 'DOWN'));
