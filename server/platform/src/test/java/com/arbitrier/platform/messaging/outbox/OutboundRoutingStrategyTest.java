package com.arbitrier.platform.messaging.outbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboundRoutingStrategyTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Test
    void strategy_resolves_destination_from_event_type() {
        OutboundRoutingStrategy strategy = message -> "order." + message.eventType().toLowerCase();

        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), "ord-1", "Order", "OrderCreated",
                "{}", "JSON", NOW, null, PublishStatus.PENDING, 0, null, null, null,
                MessageNature.EVENT);

        assertThat(strategy.resolveDestination(event)).isEqualTo("order.ordercreated");
    }

    @Test
    void strategy_can_discriminate_by_nature() {
        OutboundRoutingStrategy strategy = message -> switch (message.messageNature()) {
            case EVENT -> "events." + message.aggregateType().toLowerCase();
            case COMMAND -> "commands." + message.aggregateType().toLowerCase();
        };

        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), "ord-1", "Order", "OrderCreated",
                "{}", "JSON", NOW, null, PublishStatus.PENDING, 0, null, null, null,
                MessageNature.EVENT);

        OutboxEvent command = new OutboxEvent(
                UUID.randomUUID(), "saga-1", "Saga", "ReserveStockCommand",
                "{}", "JSON", NOW, null, PublishStatus.PENDING, 0, null, null, null,
                MessageNature.COMMAND);

        assertThat(strategy.resolveDestination(event)).isEqualTo("events.order");
        assertThat(strategy.resolveDestination(command)).isEqualTo("commands.saga");
    }
}
