package com.arbitrier.platform.messaging.outbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class OutboxEventTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Test
    void creates_valid_pending_event() {
        OutboxEvent event = new OutboxEvent(
                EVENT_ID, "AGG-1", "Order", "OrderCreatedDomainEvent",
                "{\"kind\":\"test\"}", "JSON",
                NOW, null, PublishStatus.PENDING, 0, null, null, null,
                MessageNature.EVENT);

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.aggregateId()).isEqualTo("AGG-1");
        assertThat(event.publishStatus()).isEqualTo(PublishStatus.PENDING);
        assertThat(event.attemptCount()).isZero();
        assertThat(event.publishedAt()).isNull();
        assertThat(event.messageNature()).isEqualTo(MessageNature.EVENT);
    }

    @Test
    void creates_command_message() {
        OutboxEvent command = new OutboxEvent(
                EVENT_ID, "SAGA-1", "Saga", "ReserveStockCommand",
                "{\"kind\":\"cmd\"}", "JSON",
                NOW, null, PublishStatus.PENDING, 0, null, null, null,
                MessageNature.COMMAND);

        assertThat(command.messageNature()).isEqualTo(MessageNature.COMMAND);
        assertThat(command.eventType()).isEqualTo("ReserveStockCommand");
    }

    @Test
    void creates_published_event_with_timestamp() {
        Instant published = NOW.plusSeconds(5);
        OutboxEvent event = new OutboxEvent(
                EVENT_ID, "AGG-2", "CreditReservation", "CreditApprovedDomainEvent",
                "{\"kind\":\"test\"}", "JSON",
                NOW, published, PublishStatus.PUBLISHED, 1, NOW.plusSeconds(3),
                "corr-1", "caus-1",
                MessageNature.EVENT);

        assertThat(event.publishStatus()).isEqualTo(PublishStatus.PUBLISHED);
        assertThat(event.publishedAt()).isEqualTo(published);
        assertThat(event.attemptCount()).isEqualTo(1);
        assertThat(event.lastAttempt()).isEqualTo(NOW.plusSeconds(3));
        assertThat(event.correlationId()).isEqualTo("corr-1");
        assertThat(event.causationId()).isEqualTo("caus-1");
    }

    @Test
    void creates_failed_event() {
        OutboxEvent event = new OutboxEvent(
                EVENT_ID, "AGG-3", "StockReservation", "StockReservedDomainEvent",
                "{\"kind\":\"test\"}", "JSON",
                NOW, null, PublishStatus.FAILED, 3, NOW, null, null,
                MessageNature.EVENT);

        assertThat(event.publishStatus()).isEqualTo(PublishStatus.FAILED);
        assertThat(event.attemptCount()).isEqualTo(3);
        assertThat(event.lastAttempt()).isEqualTo(NOW);
    }

    @Test
    void null_eventId_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new OutboxEvent(
                        null, "A", "Order", "Event", "{}", "JSON",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT))
                .withMessageContaining("eventId");
    }

    @Test
    void blank_aggregateId_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "", "Order", "Event", "{}", "JSON",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT))
                .withMessageContaining("aggregateId");
    }

    @Test
    void blank_aggregateType_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "  ", "Event", "{}", "JSON",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT))
                .withMessageContaining("aggregateType");
    }

    @Test
    void blank_eventType_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "", "{}", "JSON",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT))
                .withMessageContaining("eventType");
    }

    @Test
    void blank_payload_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "Event", "", "JSON",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT))
                .withMessageContaining("payload");
    }

    @Test
    void blank_payloadFormat_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "Event", "{}", "",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT))
                .withMessageContaining("payloadFormat");
    }

    @Test
    void null_occurredAt_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "Event", "{}", "JSON",
                        null, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT))
                .withMessageContaining("occurredAt");
    }

    @Test
    void null_publishStatus_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "Event", "{}", "JSON",
                        NOW, null, null, 0, null, null, null,
                        MessageNature.EVENT))
                .withMessageContaining("publishStatus");
    }

    @Test
    void null_messageNature_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "Event", "{}", "JSON",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        null))
                .withMessageContaining("messageNature");
    }

    @Test
    void nullable_fields_can_be_null() {
        OutboxEvent event = new OutboxEvent(
                EVENT_ID, "A", "Order", "Event", "{}", "JSON",
                NOW, null, PublishStatus.PENDING, 0, null, null, null,
                MessageNature.EVENT);

        assertThat(event.publishedAt()).isNull();
        assertThat(event.lastAttempt()).isNull();
        assertThat(event.correlationId()).isNull();
        assertThat(event.causationId()).isNull();
    }
}
