package com.arbitrier.platform.messaging.outbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link OutboundPayloadSerializer} is a usable functional interface
 * and that implementations can satisfy the contract.
 */
class OutboundPayloadSerializerTest {

    @Test
    void can_be_implemented_as_lambda() {
        OutboundPayloadSerializer serializer = event -> "serialized:" + event.eventType();

        OutboxEvent event = event();
        assertThat(serializer.serialize(event)).isEqualTo("serialized:OrderCreatedDomainEvent");
    }

    @Test
    void accepts_event_and_command_nature() {
        OutboundPayloadSerializer serializer = event -> event.messageNature().name();

        assertThat(serializer.serialize(event())).isEqualTo("EVENT");
        assertThat(serializer.serialize(commandEvent())).isEqualTo("COMMAND");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private OutboxEvent event() {
        return new OutboxEvent(
                UUID.randomUUID(), "order-001", "Order", "OrderCreatedDomainEvent",
                "{}", "JSON", Instant.now(), null, PublishStatus.PENDING, 0, null,
                null, null, MessageNature.EVENT);
    }

    private OutboxEvent commandEvent() {
        return new OutboxEvent(
                UUID.randomUUID(), "saga-001", "Saga", "ReserveStockCommand",
                "{}", "JSON", Instant.now(), null, PublishStatus.PENDING, 0, null,
                null, null, MessageNature.COMMAND);
    }
}
