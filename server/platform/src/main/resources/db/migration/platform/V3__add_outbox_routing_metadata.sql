-- V3: Add message_nature to outbox_events to distinguish domain events from commands.
-- Existing rows default to 'EVENT'; the check constraint mirrors MessageNature enum values.

ALTER TABLE outbox_events
    ADD COLUMN message_nature VARCHAR(20) NOT NULL DEFAULT 'EVENT';

ALTER TABLE outbox_events
    ADD CONSTRAINT outbox_message_nature_chk
        CHECK (message_nature IN ('EVENT', 'COMMAND'));
