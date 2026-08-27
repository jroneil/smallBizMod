CREATE TABLE sbp_role (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_sbp_role_name UNIQUE (name),
    CONSTRAINT ck_sbp_role_status CHECK (status IN ('ACTIVE', 'RETIRED'))
);

CREATE TABLE sbp_user_role (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_sbp_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_sbp_user_role_user FOREIGN KEY (user_id) REFERENCES sbp_user (id),
    CONSTRAINT fk_sbp_user_role_role FOREIGN KEY (role_id) REFERENCES sbp_role (id)
);

CREATE INDEX idx_sbp_user_role_role_id ON sbp_user_role (role_id);
