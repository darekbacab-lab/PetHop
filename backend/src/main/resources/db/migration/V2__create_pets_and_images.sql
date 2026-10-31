CREATE TABLE pets (
  id UUID PRIMARY KEY,
  shelter_id UUID NOT NULL REFERENCES shelter_profiles(id),
  name VARCHAR(100) NOT NULL,
  species VARCHAR(12) NOT NULL CHECK (species IN ('DOG','CAT','OTHER')),
  breed VARCHAR(120),
  sex VARCHAR(12) NOT NULL DEFAULT 'UNKNOWN' CHECK (sex IN ('MALE','FEMALE','UNKNOWN')),
  age_value SMALLINT CHECK (age_value IS NULL OR age_value >= 0),
  age_unit VARCHAR(12) CHECK (age_unit IS NULL OR age_unit IN ('DAYS','MONTHS','YEARS')),
  size VARCHAR(12) CHECK (size IS NULL OR size IN ('SMALL','MEDIUM','LARGE')),
  weight NUMERIC(6,2) CHECK (weight IS NULL OR weight > 0),
  color VARCHAR(100),
  description TEXT NOT NULL,
  personality TEXT,
  health_summary TEXT,
  sterilized BOOLEAN NOT NULL DEFAULT FALSE,
  vaccinated BOOLEAN NOT NULL DEFAULT FALSE,
  dewormed BOOLEAN NOT NULL DEFAULT FALSE,
  location VARCHAR(180),
  adoption_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK (adoption_status IN ('DRAFT','AVAILABLE','IN_PROCESS','ADOPTED','UNAVAILABLE')),
  published_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_pets_public_catalog ON pets(adoption_status, published_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_pets_shelter ON pets(shelter_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_pets_species_sex_size ON pets(species, sex, size) WHERE deleted_at IS NULL;
CREATE INDEX idx_pets_name_lower ON pets(lower(name)) WHERE deleted_at IS NULL;
CREATE TABLE pet_images (
  id UUID PRIMARY KEY,
  pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
  storage_path VARCHAR(500) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(80) NOT NULL CHECK (content_type IN ('image/jpeg','image/png','image/webp')),
  file_size BIGINT NOT NULL CHECK (file_size BETWEEN 1 AND 10485760),
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  display_order SMALLINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_pet_images_order ON pet_images(pet_id, display_order);
CREATE UNIQUE INDEX uq_pet_primary_image ON pet_images(pet_id) WHERE is_primary = TRUE;
