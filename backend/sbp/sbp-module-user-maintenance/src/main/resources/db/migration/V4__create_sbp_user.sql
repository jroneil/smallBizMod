CREATE TABLE sbp_user (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(320) NOT NULL,
    given_name VARCHAR(100) NOT NULL,
    family_name VARCHAR(100) NOT NULL,
    phone VARCHAR(40),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_sbp_user_username UNIQUE (username),
    CONSTRAINT ck_sbp_user_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_sbp_user_status ON sbp_user (status);
CREATE INDEX idx_sbp_user_family_name ON sbp_user (family_name);
