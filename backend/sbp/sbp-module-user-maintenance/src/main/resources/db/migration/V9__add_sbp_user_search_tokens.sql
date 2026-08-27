-- Add keyed blind-index search-token columns for encrypted personal fields. Tokens are
-- HMAC-SHA-256 of normalized values; they are non-reversible and contain no plaintext PII.
-- Columns start nullable so existing encrypted rows can be backfilled by the application at
-- startup; the application always populates them for new and updated rows.
ALTER TABLE sbp_user ADD COLUMN email_token VARCHAR(64);
ALTER TABLE sbp_user ADD COLUMN given_name_token VARCHAR(64);
ALTER TABLE sbp_user ADD COLUMN family_name_token VARCHAR(64);
ALTER TABLE sbp_user ADD COLUMN full_name_token VARCHAR(64);

CREATE INDEX idx_sbp_user_email_token ON sbp_user (email_token);
CREATE INDEX idx_sbp_user_given_name_token ON sbp_user (given_name_token);
CREATE INDEX idx_sbp_user_family_name_token ON sbp_user (family_name_token);
CREATE INDEX idx_sbp_user_full_name_token ON sbp_user (full_name_token);
