-- Content store for the Document module's reference database-backed BlobStorage implementation
-- (ADR-DOC-002). Content is reached only through the framework storage abstraction; this table is
-- not part of the Document metadata mapping. The storage key is the Document UUID.
CREATE TABLE sbp_document_content (
    storage_key UUID PRIMARY KEY,
    content BYTEA NOT NULL,
    stored_at TIMESTAMPTZ NOT NULL
);
