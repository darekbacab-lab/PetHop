ALTER TABLE pets
    ADD COLUMN life_stage VARCHAR(16) CHECK (life_stage IS NULL OR life_stage IN ('PUPPY','YOUNG','ADULT','SENIOR')),
    ADD COLUMN health_condition TEXT,
    ADD COLUMN adoption_story TEXT,
    ADD COLUMN background TEXT,
    ADD COLUMN home_type VARCHAR(180),
    ADD COLUMN housing_conditions TEXT,
    ADD COLUMN recommended_experience TEXT,
    ADD COLUMN adoption_requirements TEXT;

ALTER TABLE pets
    ALTER COLUMN sterilized DROP NOT NULL,
    ALTER COLUMN vaccinated DROP NOT NULL,
    ALTER COLUMN dewormed DROP NOT NULL;

CREATE TABLE pet_personality_traits (
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    position SMALLINT NOT NULL CHECK (position >= 0),
    trait VARCHAR(120) NOT NULL,
    PRIMARY KEY (pet_id, position),
    UNIQUE (pet_id, trait)
);
