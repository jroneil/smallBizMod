-- Group→Role assignment (UM-FR-11): members of a group inherit the roles assigned here.
-- Mirrors the sbp_user_role / sbp_user_group join conventions: intra-module FKs, composite PK,
-- assigned_at timestamp. A row survives role/group retirement so historical grants are preserved.
CREATE TABLE sbp_group_role (
    group_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_sbp_group_role PRIMARY KEY (group_id, role_id),
    CONSTRAINT fk_sbp_group_role_group FOREIGN KEY (group_id) REFERENCES sbp_group (id),
    CONSTRAINT fk_sbp_group_role_role FOREIGN KEY (role_id) REFERENCES sbp_role (id)
);

CREATE INDEX idx_sbp_group_role_role_id ON sbp_group_role (role_id);
