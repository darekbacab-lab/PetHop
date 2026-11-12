ALTER TABLE notifications ADD COLUMN dedupe_key VARCHAR(180);
CREATE UNIQUE INDEX uq_notifications_dedupe_key ON notifications(dedupe_key) WHERE dedupe_key IS NOT NULL;
