CREATE TABLE shipments (
    id           UUID         NOT NULL,
    order_id     UUID         NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    carrier      VARCHAR(50),
    tracking_no  VARCHAR(100),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_shipments PRIMARY KEY (id),
    CONSTRAINT uk_shipments_order UNIQUE (order_id)
);

CREATE INDEX idx_shipments_status ON shipments (status);
CREATE INDEX idx_shipments_created_at ON shipments (created_at DESC);
