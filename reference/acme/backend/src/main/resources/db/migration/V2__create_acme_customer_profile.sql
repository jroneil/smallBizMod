CREATE TABLE acme_customer_profile (
    customer_id UUID PRIMARY KEY,
    service_region VARCHAR(100) NOT NULL,
    account_notes VARCHAR(2000)
);
