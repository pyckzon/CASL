CREATE TABLE outbox (
    id           UUID         NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ,
    CONSTRAINT pk_outbox PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_status ON outbox (status);
CREATE INDEX idx_outbox_created_at ON outbox (created_at);
