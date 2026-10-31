CREATE TABLE pet_health_records (
  id UUID PRIMARY KEY,
  pet_id UUID NOT NULL REFERENCES pets(id),
  record_type VARCHAR(32) NOT NULL CHECK (record_type IN ('VACCINATION','DEWORMING','STERILIZATION','MEDICAL_CONSULTATION','TREATMENT','OTHER')),
  title VARCHAR(180) NOT NULL,
  description TEXT,
  performed_at DATE NOT NULL,
  next_due_at DATE,
  veterinarian_name VARCHAR(160),
  document_path VARCHAR(500),
  created_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_health_pet_date ON pet_health_records(pet_id, performed_at DESC);
CREATE INDEX idx_health_due ON pet_health_records(next_due_at) WHERE next_due_at IS NOT NULL;
