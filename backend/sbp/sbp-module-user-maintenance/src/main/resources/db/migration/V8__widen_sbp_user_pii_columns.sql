-- Widen personal-data columns to hold AES-256-GCM ciphertext (Base64 of IV || ciphertext||tag).
-- Existing development rows are plaintext and are not migrated here; the reference deployment
-- resets its development database for the encryption slice.
ALTER TABLE sbp_user ALTER COLUMN email TYPE VARCHAR(500);
ALTER TABLE sbp_user ALTER COLUMN given_name TYPE VARCHAR(250);
ALTER TABLE sbp_user ALTER COLUMN family_name TYPE VARCHAR(250);
ALTER TABLE sbp_user ALTER COLUMN phone TYPE VARCHAR(150);
