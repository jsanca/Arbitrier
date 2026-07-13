-- V2: Outbox and Inbox tables (platform-owned, created in each service schema).
-- Table names are unqualified; Flyway resolves them to the current service schema.

CREATE TABLE outbox_events (
    id              UUID            NOT NULL,
    aggregate_id    VARCHAR(255)    NOT NULL,
    aggregate_type  VARCHAR(100)    NOT NULL,
    event_type      VARCHAR(255)    NOT NULL,
    payload         TEXT            NOT NULL,
    payload_format  VARCHAR(20)     NOT NULL DEFAULT 'JSON',
    occurred_at     TIMESTAMPTZ     NOT NULL,
    published_at    TIMESTAMPTZ,
    publish_status  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    attempt_count   INTEGER         NOT NULL DEFAULT 0,
    last_attempt    TIMESTAMPTZ,
    correlation_id  VARCHAR(255),
    causation_id    VARCHAR(255),
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_outbox_events PRIMARY KEY (id),
    CONSTRAINT outbox_publish_status_chk CHECK (publish_status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_outbox_publish_status ON outbox_events (publish_status);
CREATE INDEX idx_outbox_occurred_at    ON outbox_events (occurred_at);
CREATE INDEX idx_outbox_aggregate_id   ON outbox_events (aggregate_id);

CREATE TABLE inbox_events (
    id                  UUID            NOT NULL,
    consumer_id         VARCHAR(255)    NOT NULL,
    received_at         TIMESTAMPTZ     NOT NULL,
    processed_at        TIMESTAMPTZ,
    processing_status   VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    correlation_id      VARCHAR(255),
    payload_hash        VARCHAR(255),
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_inbox_events PRIMARY KEY (id),
    CONSTRAINT inbox_processing_status_chk CHECK (processing_status IN ('PENDING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_inbox_consumer_id       ON inbox_events (consumer_id);
CREATE INDEX idx_inbox_processed_at      ON inbox_events (processed_at);
CREATE INDEX idx_inbox_event_id          ON inbox_events (id);
