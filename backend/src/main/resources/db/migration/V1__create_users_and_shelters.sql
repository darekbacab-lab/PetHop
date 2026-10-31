CREATE TABLE users (
  id UUID PRIMARY KEY,
  full_name VARCHAR(160) NOT NULL,
  email VARCHAR(254) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  phone VARCHAR(32),
  role VARCHAR(16) NOT NULL CHECK (role IN ('ADOPTER','SHELTER','ADMIN')),
  account_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (account_status IN ('ACTIVE','LOCKED','DISABLED')),
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_users_email UNIQUE (email)
);
CREATE TABLE shelter_profiles (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL UNIQUE REFERENCES users(id),
  organization_name VARCHAR(180) NOT NULL,
  description TEXT,
  address VARCHAR(240),
  city VARCHAR(100),
  state VARCHAR(100),
  postal_code VARCHAR(20),
  contact_phone VARCHAR(32),
  verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING','IN_REVIEW','VERIFIED','REJECTED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_role_status ON users(role, account_status);
