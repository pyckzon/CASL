CREATE TABLE notifications (
    id           UUID         NOT NULL,
    event_id     UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    channel      VARCHAR(50)  NOT NULL,
    recipient    VARCHAR(320) NOT NULL,
    subject      VARCHAR(200),
    body         TEXT,
    status       VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT uk_notifications_event_id UNIQUE (event_id)
);

CREATE TABLE inbox (
    event_id     UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_inbox PRIMARY KEY (event_id)
);

CREATE INDEX idx_notifications_event_type ON notifications (event_type);
CREATE INDEX idx_notifications_created_at ON notifications (created_at DESC);
