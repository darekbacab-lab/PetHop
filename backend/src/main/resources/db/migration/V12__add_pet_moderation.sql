ALTER TABLE pets ADD COLUMN moderation_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE pets ADD COLUMN moderation_reason TEXT;
ALTER TABLE pets ADD COLUMN moderated_at TIMESTAMPTZ;
ALTER TABLE pets ADD CONSTRAINT ck_pets_moderation_status CHECK (moderation_status IN ('PENDING','APPROVED','REJECTED','CHANGES_REQUESTED'));
CREATE INDEX idx_pets_moderation_queue ON pets(moderation_status, created_at DESC) WHERE deleted_at IS NULL;
