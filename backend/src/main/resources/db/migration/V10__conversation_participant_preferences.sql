ALTER TABLE conversation_participants
  ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN hidden_at TIMESTAMPTZ;

CREATE INDEX idx_participants_inbox_order
  ON conversation_participants(user_id, pinned DESC, joined_at DESC)
  WHERE hidden_at IS NULL;
