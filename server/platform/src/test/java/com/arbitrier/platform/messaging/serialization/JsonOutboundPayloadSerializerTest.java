package com.arbitrier.platform.messaging.serialization;

import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for {@link JsonOutboundPayloadSerializer}.
 */
class JsonOutboundPayloadSerializerTest {

    private final JsonOutboundPayloadSerializer serializer = new JsonOutboundPayloadSerializer();

    @Test
    void returns_event_payload_unchanged() {
        String json = "{\"orderId\":\"order-001\",\"quantity\":10}";
        OutboxEvent event = eventWithPayload(json);

        assertThat(serializer.serialize(event)).isEqualTo(json);
    }

    @Test
    void returns_command_payload_unchanged() {
        String json = "{\"sku\":\"SKU-A\",\"quantity\":5}";
        OutboxEvent command = commandWithPayload(json);

        assertThat(serializer.serialize(command)).isEqualTo(json);
    }

    @Test
    void rejects_null_event() {
        assertThatNullPointerException()
                .isThrownBy(() -> serializer.serialize(null))
                .withMessageContaining("event");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private OutboxEvent eventWithPayload(String payload) {
        return new OutboxEvent(
                UUID.randomUUID(), "order-001", "Order", "OrderCreatedDomainEvent",
                payload, "JSON", Instant.now(), null, PublishStatus.PENDING, 0, null,
                null, null, MessageNature.EVENT, null, null);
    }

    private OutboxEvent commandWithPayload(String payload) {
        return new OutboxEvent(
                UUID.randomUUID(), "saga-001", "Saga", "ReserveStockCommand",
                payload, "JSON", Instant.now(), null, PublishStatus.PENDING, 0, null,
                null, null, MessageNature.COMMAND, null, null);
    }
}
