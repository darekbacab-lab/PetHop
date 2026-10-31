CREATE TABLE adoptions (
  id UUID PRIMARY KEY,
  application_id UUID NOT NULL UNIQUE REFERENCES adoption_applications(id),
  pet_id UUID NOT NULL REFERENCES pets(id),
  adopter_id UUID NOT NULL REFERENCES users(id),
  shelter_id UUID NOT NULL REFERENCES shelter_profiles(id),
  adoption_date DATE NOT NULL,
  agreement_notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_active_adoption_pet ON adoptions(pet_id);
CREATE INDEX idx_adoptions_adopter ON adoptions(adopter_id, adoption_date DESC);
CREATE INDEX idx_adoptions_shelter ON adoptions(shelter_id, adoption_date DESC);
CREATE TABLE follow_up_reports (
  id UUID PRIMARY KEY,
  adoption_id UUID NOT NULL REFERENCES adoptions(id),
  created_by UUID NOT NULL REFERENCES users(id),
  report_date DATE NOT NULL,
  pet_condition VARCHAR(32) NOT NULL CHECK (pet_condition IN ('GOOD','ADAPTING','NEEDS_ATTENTION')),
  behavior_notes TEXT,
  health_notes TEXT,
  general_notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_followups_adoption ON follow_up_reports(adoption_id, report_date DESC);
CREATE TABLE follow_up_media (
  id UUID PRIMARY KEY,
  follow_up_id UUID NOT NULL REFERENCES follow_up_reports(id) ON DELETE CASCADE,
  storage_path VARCHAR(500) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(80) NOT NULL CHECK (content_type IN ('image/jpeg','image/png','image/webp')),
  file_size BIGINT NOT NULL CHECK (file_size BETWEEN 1 AND 10485760),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
