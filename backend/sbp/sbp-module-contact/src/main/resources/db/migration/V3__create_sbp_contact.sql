CREATE TABLE sbp_contact (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    role VARCHAR(100),
    email VARCHAR(200),
    phone VARCHAR(40),
    status VARCHAR(20) NOT NULL,
    CONSTRAINT ck_sbp_contact_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_sbp_contact_customer_id ON sbp_contact (customer_id);
