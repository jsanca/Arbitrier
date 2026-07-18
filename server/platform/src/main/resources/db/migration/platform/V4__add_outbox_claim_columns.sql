-- V4: Add CLAIMED status and claim metadata columns to outbox_events.
-- claimed_by identifies the worker that temporarily owns the event.
-- claimed_at records when the claim was made.
-- The publish_status check constraint is extended to include CLAIMED.

ALTER TABLE outbox_events
    ADD COLUMN claimed_by VARCHAR(255);

ALTER TABLE outbox_events
    ADD COLUMN claimed_at TIMESTAMPTZ;

ALTER TABLE outbox_events
    DROP CONSTRAINT outbox_publish_status_chk;

ALTER TABLE outbox_events
    ADD CONSTRAINT outbox_publish_status_chk
        CHECK (publish_status IN ('PENDING', 'CLAIMED', 'PUBLISHED', 'FAILED'));
