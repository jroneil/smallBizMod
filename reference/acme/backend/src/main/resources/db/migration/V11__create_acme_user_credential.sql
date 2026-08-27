-- Acme-owned local credential store. Stores only a one-way password hash plus failed-attempt
-- accounting, keyed by the stable SBP User id. Passwords are never stored in plaintext.
CREATE TABLE acme_user_credential (
    user_id UUID PRIMARY KEY,
    password_hash VARCHAR(200) NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL
);
