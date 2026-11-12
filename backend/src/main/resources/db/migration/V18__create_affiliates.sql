CREATE TABLE affiliates (
  id UUID PRIMARY KEY,
  name VARCHAR(180) NOT NULL,
  category VARCHAR(40) NOT NULL,
  description TEXT,
  location VARCHAR(180),
  website_url VARCHAR(500),
  discount_code VARCHAR(80),
  discount_text VARCHAR(240),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_affiliates_active_category ON affiliates(active, category, name);
