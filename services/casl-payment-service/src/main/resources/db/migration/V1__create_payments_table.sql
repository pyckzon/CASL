CREATE TABLE payments (
    id               UUID          NOT NULL,
    idempotency_key  VARCHAR(128)  NOT NULL,
    amount           NUMERIC(19,2) NOT NULL,
    currency         VARCHAR(3)    NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    failure_reason   VARCHAR(500),
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_created_at ON payments (created_at DESC);
