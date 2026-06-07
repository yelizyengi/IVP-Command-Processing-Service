CREATE TABLE outbox_entry (
    id           UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    topic        VARCHAR(255) NOT NULL,
    payload      TEXT NOT NULL,
    published    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_unpublished ON outbox_entry(published, created_at) WHERE published = FALSE;
