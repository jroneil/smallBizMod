-- Add the mandatory password-change flag to the Acme credential record. Existing credentials
-- preserve their current hashes; the new column defaults to false.
ALTER TABLE acme_user_credential ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;
