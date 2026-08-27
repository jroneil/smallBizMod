CREATE TABLE sbp_document (
    id UUID PRIMARY KEY,
    customer_id UUID,
    job_id UUID,
    display_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_sbp_document_exactly_one_owner CHECK (
        (customer_id IS NOT NULL AND job_id IS NULL)
        OR (customer_id IS NULL AND job_id IS NOT NULL)
    )
);

-- No foreign keys to sbp_customer or sbp_job: cross-module owner references are validated through
-- the public Customer and Job contracts and stored as stable owner UUIDs (ADR-DOC-001, ADR-006).
CREATE INDEX idx_sbp_document_customer_id ON sbp_document (customer_id);
CREATE INDEX idx_sbp_document_job_id ON sbp_document (job_id);
