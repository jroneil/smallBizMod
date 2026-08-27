CREATE TABLE sbp_document_audit_event (
    id UUID PRIMARY KEY,
    actor VARCHAR(100),
    subject_type VARCHAR(20) NOT NULL,
    subject_id VARCHAR(64) NOT NULL,
    action VARCHAR(40) NOT NULL,
    changed_fields VARCHAR(500),
    detail VARCHAR(200),
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_sbp_document_audit_occurred ON sbp_document_audit_event (occurred_at);
CREATE INDEX idx_sbp_document_audit_subject ON sbp_document_audit_event (subject_type, subject_id);
