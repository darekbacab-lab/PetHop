CREATE TABLE adoption_applications (
  id UUID PRIMARY KEY,
  pet_id UUID NOT NULL REFERENCES pets(id),
  adopter_id UUID NOT NULL REFERENCES users(id),
  shelter_id UUID NOT NULL REFERENCES shelter_profiles(id),
  status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','APPROVED','REJECTED','WITHDRAWN')),
  motivation TEXT NOT NULL,
  housing_type VARCHAR(100),
  has_other_pets BOOLEAN,
  has_children BOOLEAN,
  experience TEXT,
  availability TEXT,
  additional_information TEXT,
  submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  reviewed_at TIMESTAMPTZ,
  reviewed_by UUID REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_applications_adopter ON adoption_applications(adopter_id, created_at DESC);
CREATE INDEX idx_applications_shelter ON adoption_applications(shelter_id, status, created_at DESC);
CREATE INDEX idx_applications_pet ON adoption_applications(pet_id, status);
CREATE UNIQUE INDEX uq_active_application ON adoption_applications(adopter_id, pet_id) WHERE status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','APPROVED');
CREATE TABLE application_answers (
  id UUID PRIMARY KEY,
  application_id UUID NOT NULL REFERENCES adoption_applications(id) ON DELETE CASCADE,
  question_key VARCHAR(100) NOT NULL,
  question_text VARCHAR(500) NOT NULL,
  answer_text TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_application_answer UNIQUE(application_id, question_key)
);
CREATE TABLE application_status_history (
  id UUID PRIMARY KEY,
  application_id UUID NOT NULL REFERENCES adoption_applications(id),
  previous_status VARCHAR(20),
  new_status VARCHAR(20) NOT NULL CHECK (new_status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','APPROVED','REJECTED','WITHDRAWN')),
  changed_by UUID NOT NULL REFERENCES users(id),
  comment TEXT,
  changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_application_history_date ON application_status_history(application_id, changed_at);
