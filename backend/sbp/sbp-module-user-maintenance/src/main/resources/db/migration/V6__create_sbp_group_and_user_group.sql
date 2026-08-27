CREATE TABLE sbp_group (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_sbp_group_name UNIQUE (name),
    CONSTRAINT ck_sbp_group_status CHECK (status IN ('ACTIVE', 'RETIRED'))
);

CREATE TABLE sbp_user_group (
    user_id UUID NOT NULL,
    group_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_sbp_user_group PRIMARY KEY (user_id, group_id),
    CONSTRAINT fk_sbp_user_group_user FOREIGN KEY (user_id) REFERENCES sbp_user (id),
    CONSTRAINT fk_sbp_user_group_group FOREIGN KEY (group_id) REFERENCES sbp_group (id)
);

CREATE INDEX idx_sbp_user_group_group_id ON sbp_user_group (group_id);
