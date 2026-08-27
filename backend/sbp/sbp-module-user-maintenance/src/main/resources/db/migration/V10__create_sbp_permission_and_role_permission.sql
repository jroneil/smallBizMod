CREATE TABLE sbp_permission (
    id UUID PRIMARY KEY,
    key VARCHAR(200) NOT NULL,
    description VARCHAR(500) NOT NULL,
    CONSTRAINT uq_sbp_permission_key UNIQUE (key)
);

CREATE TABLE sbp_role_permission (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_sbp_role_permission PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_sbp_role_permission_role FOREIGN KEY (role_id) REFERENCES sbp_role (id),
    CONSTRAINT fk_sbp_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sbp_permission (id)
);

CREATE INDEX idx_sbp_role_permission_permission_id ON sbp_role_permission (permission_id);
