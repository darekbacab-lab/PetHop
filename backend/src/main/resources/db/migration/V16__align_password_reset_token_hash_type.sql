ALTER TABLE password_reset_tokens
  ALTER COLUMN token_hash TYPE VARCHAR(64)
  USING btrim(token_hash)::VARCHAR(64);
