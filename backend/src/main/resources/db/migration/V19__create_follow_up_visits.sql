CREATE TABLE follow_up_visits (
  id UUID PRIMARY KEY,
  adoption_id UUID NOT NULL REFERENCES adoptions(id),
  created_by UUID NOT NULL REFERENCES users(id),
  visit_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED','COMPLETED','CANCELLED')),
  outcome VARCHAR(120),
  notes TEXT,
  next_action TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_follow_up_visits_adoption ON follow_up_visits(adoption_id, visit_date DESC);
