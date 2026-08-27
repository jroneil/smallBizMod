CREATE TABLE sbp_job (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(10) NOT NULL,
    scheduled_date DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_sbp_job_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_sbp_job_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH'))
);

-- No foreign key to sbp_customer: cross-module references are validated through the public
-- Customer contract and stored as a stable Customer UUID (ADR-JOB-001, ADR-006).
CREATE INDEX idx_sbp_job_customer_id ON sbp_job (customer_id);
CREATE INDEX idx_sbp_job_status ON sbp_job (status);
CREATE INDEX idx_sbp_job_priority ON sbp_job (priority);
