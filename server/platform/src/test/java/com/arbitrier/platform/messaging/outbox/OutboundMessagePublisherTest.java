package com.arbitrier.platform.messaging.outbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboundMessagePublisherTest {

    @Test
    void is_a_functional_interface_implementable_as_lambda() {
        List<OutboxEvent> captured = new ArrayList<>();
        OutboundMessagePublisher publisher = captured::add;

        OutboxEvent event = pendingEvent(MessageNature.EVENT);
        publisher.publish(event);

        assertThat(captured).containsExactly(event);
    }

    @Test
    void accepts_command_nature() {
        List<OutboxEvent> captured = new ArrayList<>();
        OutboundMessagePublisher publisher = captured::add;

        OutboxEvent command = pendingEvent(MessageNature.COMMAND);
        publisher.publish(command);

        assertThat(captured.getFirst().messageNature()).isEqualTo(MessageNature.COMMAND);
    }

    private OutboxEvent pendingEvent(MessageNature nature) {
        return new OutboxEvent(
                UUID.randomUUID(), "agg-1", "Order", "OrderCreatedDomainEvent",
                "{}", "JSON",
                Instant.parse("2026-01-15T10:00:00Z"), null,
                PublishStatus.PENDING, 0, null, null, null,
                nature);
    }
}
