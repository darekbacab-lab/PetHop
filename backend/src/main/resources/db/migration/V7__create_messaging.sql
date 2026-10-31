CREATE TABLE conversations (
  id UUID PRIMARY KEY,
  application_id UUID UNIQUE REFERENCES adoption_applications(id),
  pet_id UUID REFERENCES pets(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_conversations_pet ON conversations(pet_id) WHERE pet_id IS NOT NULL;
CREATE TABLE conversation_participants (
  id UUID PRIMARY KEY,
  conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id),
  joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_conversation_participant UNIQUE(conversation_id, user_id)
);
CREATE INDEX idx_participants_user ON conversation_participants(user_id, joined_at DESC);
CREATE TABLE messages (
  id UUID PRIMARY KEY,
  conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
  sender_id UUID NOT NULL REFERENCES users(id),
  content TEXT NOT NULL CHECK (length(trim(content)) BETWEEN 1 AND 5000),
  read_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_conversation_date ON messages(conversation_id, created_at DESC);
