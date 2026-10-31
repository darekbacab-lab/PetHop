-- Hibernate maps @OrderColumn to INTEGER. V9 originally created this as SMALLINT,
-- which made schema validation fail on PostgreSQL. Widen the type without changing
-- existing ordering values or personality records.
ALTER TABLE pet_personality_traits
    ALTER COLUMN position TYPE INTEGER
    USING position::INTEGER;
