CREATE TABLE orders (
    id             UUID          NOT NULL,
    user_id        UUID,
    status         VARCHAR(20)   NOT NULL,
    total_amount   NUMERIC(19,2) NOT NULL,
    currency       VARCHAR(3)    NOT NULL,
    payment_id     UUID,
    failure_reason VARCHAR(500),
    lines          JSONB         NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at DESC);
