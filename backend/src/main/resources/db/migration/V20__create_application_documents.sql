CREATE TABLE application_documents (
  id UUID PRIMARY KEY,
  application_id UUID NOT NULL REFERENCES adoption_applications(id) ON DELETE CASCADE,
  uploaded_by UUID NOT NULL REFERENCES users(id),
  document_type VARCHAR(40) NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(80) NOT NULL CHECK (content_type IN ('application/pdf','image/jpeg','image/png','image/webp')),
  file_size BIGINT NOT NULL CHECK (file_size BETWEEN 1 AND 10485760),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_application_documents_application ON application_documents(application_id, created_at DESC);
