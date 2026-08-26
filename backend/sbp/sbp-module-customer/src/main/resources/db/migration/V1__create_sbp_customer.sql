CREATE TABLE sbp_customer (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT ck_sbp_customer_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
