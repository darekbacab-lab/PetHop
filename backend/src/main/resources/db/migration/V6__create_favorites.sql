CREATE TABLE favorites (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  pet_id UUID NOT NULL REFERENCES pets(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_favorite_user_pet UNIQUE(user_id, pet_id)
);
CREATE INDEX idx_favorites_user ON favorites(user_id, created_at DESC);
