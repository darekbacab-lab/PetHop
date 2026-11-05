CREATE TABLE admin_audit_events (
  id UUID PRIMARY KEY,
  admin_id UUID NOT NULL REFERENCES users(id),
  action VARCHAR(60) NOT NULL,
  entity_type VARCHAR(40) NOT NULL,
  entity_id UUID NOT NULL,
  previous_state TEXT,
  new_state TEXT,
  reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_admin_audit_created ON admin_audit_events(created_at DESC);

CREATE TABLE user_reports (
  id UUID PRIMARY KEY,
  reporter_id UUID NOT NULL REFERENCES users(id),
  pet_id UUID NOT NULL REFERENCES pets(id),
  category VARCHAR(30) NOT NULL CHECK (category IN ('FRAUD','MISLEADING_INFORMATION','ABUSE','INAPPROPRIATE_CONTENT','OTHER')),
  description TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED','UNDER_REVIEW','RESOLVED','DISMISSED')),
  resolution_details TEXT,
  resolved_by UUID REFERENCES users(id),
  resolved_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_user_reports_status ON user_reports(status, created_at DESC);
CREATE UNIQUE INDEX uq_open_user_pet_report ON user_reports(reporter_id, pet_id) WHERE status IN ('SUBMITTED','UNDER_REVIEW');
