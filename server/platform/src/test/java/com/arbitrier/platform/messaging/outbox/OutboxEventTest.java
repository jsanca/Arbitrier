package com.arbitrier.platform.messaging.outbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class OutboxEventTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final String WORKER = "worker-1";

    // ── construction ─────────────────────────────────────────────────────────

    @Test
    void creates_valid_pending_event() {
        OutboxEvent event = pending();

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.aggregateId()).isEqualTo("AGG-1");
        assertThat(event.publishStatus()).isEqualTo(PublishStatus.PENDING);
        assertThat(event.attemptCount()).isZero();
        assertThat(event.publishedAt()).isNull();
        assertThat(event.messageNature()).isEqualTo(MessageNature.EVENT);
        assertThat(event.claimedBy()).isNull();
        assertThat(event.claimedAt()).isNull();
    }

    @Test
    void creates_command_message() {
        OutboxEvent command = new OutboxEvent(
                EVENT_ID, "SAGA-1", "Saga", "ReserveStockCommand",
                "{\"kind\":\"cmd\"}", "JSON",
                NOW, null, PublishStatus.PENDING, 0, null, null, null,
                MessageNature.COMMAND, null, null);

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
                MessageNature.EVENT, null, null);

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
                MessageNature.EVENT, null, null);

        assertThat(event.publishStatus()).isEqualTo(PublishStatus.FAILED);
        assertThat(event.attemptCount()).isEqualTo(3);
        assertThat(event.lastAttempt()).isEqualTo(NOW);
    }

    // ── existing field validation ─────────────────────────────────────────────

    @Test
    void null_eventId_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new OutboxEvent(
                        null, "A", "Order", "Event", "{}", "JSON",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT, null, null))
                .withMessageContaining("eventId");
    }

    @Test
    void blank_aggregateId_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "", "Order", "Event", "{}", "JSON",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT, null, null))
                .withMessageContaining("aggregateId");
    }

    @Test
    void blank_aggregateType_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "  ", "Event", "{}", "JSON",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT, null, null))
                .withMessageContaining("aggregateType");
    }

    @Test
    void blank_eventType_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "", "{}", "JSON",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT, null, null))
                .withMessageContaining("eventType");
    }

    @Test
    void blank_payload_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "Event", "", "JSON",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT, null, null))
                .withMessageContaining("payload");
    }

    @Test
    void blank_payloadFormat_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "Event", "{}", "",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT, null, null))
                .withMessageContaining("payloadFormat");
    }

    @Test
    void null_occurredAt_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "Event", "{}", "JSON",
                        null, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT, null, null))
                .withMessageContaining("occurredAt");
    }

    @Test
    void null_publishStatus_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "Event", "{}", "JSON",
                        NOW, null, null, 0, null, null, null,
                        MessageNature.EVENT, null, null))
                .withMessageContaining("publishStatus");
    }

    @Test
    void null_messageNature_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "Event", "{}", "JSON",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        null, null, null))
                .withMessageContaining("messageNature");
    }

    @Test
    void nullable_fields_can_be_null() {
        OutboxEvent event = pending();

        assertThat(event.publishedAt()).isNull();
        assertThat(event.lastAttempt()).isNull();
        assertThat(event.correlationId()).isNull();
        assertThat(event.causationId()).isNull();
        assertThat(event.claimedBy()).isNull();
        assertThat(event.claimedAt()).isNull();
    }

    // ── claim invariants ─────────────────────────────────────────────────────

    @Test
    void claimed_event_requires_claimedBy() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "Event", "{}", "JSON",
                        NOW, null, PublishStatus.CLAIMED, 0, null, null, null,
                        MessageNature.EVENT, null, NOW))
                .withMessageContaining("claimedBy");
    }

    @Test
    void claimed_event_requires_claimedAt() {
        assertThatNullPointerException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "Event", "{}", "JSON",
                        NOW, null, PublishStatus.CLAIMED, 0, null, null, null,
                        MessageNature.EVENT, WORKER, null))
                .withMessageContaining("claimedAt");
    }

    @Test
    void pending_event_rejects_claimedBy() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "Event", "{}", "JSON",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT, WORKER, null))
                .withMessageContaining("claimedBy");
    }

    @Test
    void pending_event_rejects_claimedAt() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxEvent(
                        EVENT_ID, "A", "Order", "Event", "{}", "JSON",
                        NOW, null, PublishStatus.PENDING, 0, null, null, null,
                        MessageNature.EVENT, null, NOW))
                .withMessageContaining("claimedAt");
    }

    @Test
    void valid_claimed_event_is_constructed() {
        OutboxEvent event = new OutboxEvent(
                EVENT_ID, "A", "Order", "Event", "{}", "JSON",
                NOW, null, PublishStatus.CLAIMED, 0, null, null, null,
                MessageNature.EVENT, WORKER, NOW);

        assertThat(event.publishStatus()).isEqualTo(PublishStatus.CLAIMED);
        assertThat(event.claimedBy()).isEqualTo(WORKER);
        assertThat(event.claimedAt()).isEqualTo(NOW);
    }

    // ── claim() transition ────────────────────────────────────────────────────

    @Test
    void pending_event_can_be_claimed() {
        OutboxEvent claimed = pending().claim(WORKER, NOW);

        assertThat(claimed.publishStatus()).isEqualTo(PublishStatus.CLAIMED);
        assertThat(claimed.claimedBy()).isEqualTo(WORKER);
        assertThat(claimed.claimedAt()).isEqualTo(NOW);
    }

    @Test
    void claimed_event_records_worker_and_timestamp() {
        OutboxEvent claimed = pending().claim("worker-42", NOW.plusSeconds(1));

        assertThat(claimed.claimedBy()).isEqualTo("worker-42");
        assertThat(claimed.claimedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void already_claimed_event_cannot_be_claimed_again() {
        OutboxEvent claimed = pending().claim(WORKER, NOW);

        assertThatIllegalStateException()
                .isThrownBy(() -> claimed.claim("other-worker", NOW.plusSeconds(1)));
    }

    @Test
    void published_event_cannot_be_claimed() {
        OutboxEvent published = new OutboxEvent(
                EVENT_ID, "A", "Order", "Event", "{}", "JSON",
                NOW, NOW, PublishStatus.PUBLISHED, 1, NOW, null, null,
                MessageNature.EVENT, null, null);

        assertThatIllegalStateException()
                .isThrownBy(() -> published.claim(WORKER, NOW));
    }

    @Test
    void failed_event_cannot_be_claimed() {
        OutboxEvent failed = new OutboxEvent(
                EVENT_ID, "A", "Order", "Event", "{}", "JSON",
                NOW, null, PublishStatus.FAILED, 1, NOW, null, null,
                MessageNature.EVENT, null, null);

        assertThatIllegalStateException()
                .isThrownBy(() -> failed.claim(WORKER, NOW));
    }

    @Test
    void blank_workerId_rejected_on_claim() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> pending().claim("  ", NOW))
                .withMessageContaining("workerId");
    }

    @Test
    void null_claimedAt_rejected_on_claim() {
        assertThatNullPointerException()
                .isThrownBy(() -> pending().claim(WORKER, null))
                .withMessageContaining("claimedAt");
    }

    // ── markPublished() transition ────────────────────────────────────────────

    @Test
    void markPublished_from_claimed_clears_claim_metadata() {
        OutboxEvent published = pending().claim(WORKER, NOW).markPublished(NOW.plusSeconds(5));

        assertThat(published.publishStatus()).isEqualTo(PublishStatus.PUBLISHED);
        assertThat(published.publishedAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(published.claimedBy()).isNull();
        assertThat(published.claimedAt()).isNull();
    }

    @Test
    void markPublished_requires_claimed_status() {
        assertThatIllegalStateException()
                .isThrownBy(() -> pending().markPublished(NOW));
    }

    // ── markFailed() transition ───────────────────────────────────────────────

    @Test
    void markFailed_from_claimed_clears_claim_metadata() {
        OutboxEvent failed = pending().claim(WORKER, NOW).markFailed(NOW.plusSeconds(3));

        assertThat(failed.publishStatus()).isEqualTo(PublishStatus.FAILED);
        assertThat(failed.lastAttempt()).isEqualTo(NOW.plusSeconds(3));
        assertThat(failed.attemptCount()).isEqualTo(1);
        assertThat(failed.claimedBy()).isNull();
        assertThat(failed.claimedAt()).isNull();
    }

    @Test
    void markFailed_requires_claimed_status() {
        assertThatIllegalStateException()
                .isThrownBy(() -> pending().markFailed(NOW));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private OutboxEvent pending() {
        return new OutboxEvent(
                EVENT_ID, "AGG-1", "Order", "OrderCreatedDomainEvent",
                "{\"kind\":\"test\"}", "JSON",
                NOW, null, PublishStatus.PENDING, 0, null, null, null,
                MessageNature.EVENT, null, null);
    }
}
