CREATE TABLE pet_moderation_history (
  id UUID PRIMARY KEY,
  pet_id UUID NOT NULL REFERENCES pets(id),
  admin_id UUID NOT NULL REFERENCES users(id),
  action VARCHAR(24) NOT NULL CHECK (action IN ('APPROVE','REJECT','REQUEST_CORRECTIONS')),
  previous_status VARCHAR(20) NOT NULL CHECK (previous_status IN ('PENDING','APPROVED','REJECTED','CHANGES_REQUESTED')),
  new_status VARCHAR(20) NOT NULL CHECK (new_status IN ('PENDING','APPROVED','REJECTED','CHANGES_REQUESTED')),
  reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_pet_moderation_history_pet ON pet_moderation_history(pet_id, created_at DESC);
CREATE INDEX idx_pet_moderation_history_admin ON pet_moderation_history(admin_id, created_at DESC);
