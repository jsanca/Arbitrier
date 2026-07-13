package com.arbitrier.platform.messaging.inbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class InboxEventTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Test
    void creates_valid_pending_event() {
        InboxEvent event = new InboxEvent(
                EVENT_ID, "inventory-service", NOW, null,
                ProcessingStatus.PENDING, null, null);

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.consumerId()).isEqualTo("inventory-service");
        assertThat(event.receivedAt()).isEqualTo(NOW);
        assertThat(event.processingStatus()).isEqualTo(ProcessingStatus.PENDING);
        assertThat(event.processedAt()).isNull();
    }

    @Test
    void creates_processed_event() {
        Instant processed = NOW.plusSeconds(2);
        InboxEvent event = new InboxEvent(
                EVENT_ID, "credit-service", NOW, processed,
                ProcessingStatus.PROCESSED, "corr-1", "sha256-abc");

        assertThat(event.processingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
        assertThat(event.processedAt()).isEqualTo(processed);
        assertThat(event.correlationId()).isEqualTo("corr-1");
        assertThat(event.payloadHash()).isEqualTo("sha256-abc");
    }

    @Test
    void creates_failed_event() {
        InboxEvent event = new InboxEvent(
                EVENT_ID, "orchestrator-service", NOW, null,
                ProcessingStatus.FAILED, null, null);

        assertThat(event.processingStatus()).isEqualTo(ProcessingStatus.FAILED);
    }

    @Test
    void null_eventId_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new InboxEvent(
                        null, "consumer", NOW, null,
                        ProcessingStatus.PENDING, null, null))
                .withMessageContaining("eventId");
    }

    @Test
    void blank_consumerId_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InboxEvent(
                        EVENT_ID, "", NOW, null,
                        ProcessingStatus.PENDING, null, null))
                .withMessageContaining("consumerId");
    }

    @Test
    void null_receivedAt_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new InboxEvent(
                        EVENT_ID, "consumer", null, null,
                        ProcessingStatus.PENDING, null, null))
                .withMessageContaining("receivedAt");
    }

    @Test
    void null_processingStatus_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new InboxEvent(
                        EVENT_ID, "consumer", NOW, null,
                        null, null, null))
                .withMessageContaining("processingStatus");
    }

    @Test
    void nullable_fields_can_be_null() {
        InboxEvent event = new InboxEvent(
                EVENT_ID, "consumer", NOW, null,
                ProcessingStatus.PENDING, null, null);

        assertThat(event.processedAt()).isNull();
        assertThat(event.correlationId()).isNull();
        assertThat(event.payloadHash()).isNull();
    }
}
