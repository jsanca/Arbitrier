package com.arbitrier.platform.messaging.retry;

import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LoggingDeadMessageHandler}.
 */
class LoggingDeadMessageHandlerTest {

    private final LoggingDeadMessageHandler handler = new LoggingDeadMessageHandler();

    @Test
    void handle_completes_normally() {
        var stage = handler.handle(context());

        assertThat(stage.toCompletableFuture().isCompletedExceptionally()).isFalse();
    }

    @Test
    void handle_returns_void_result() {
        var result = handler.handle(context()).toCompletableFuture().join();

        assertThat(result).isNull();
    }

    @Test
    void handle_does_not_throw() {
        // No assertion needed — if this throws the test fails automatically.
        handler.handle(context());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private DeadMessageContext context() {
        return new DeadMessageContext(outboxEvent(), 3, new RuntimeException("final failure"), Instant.now());
    }

    private OutboxEvent outboxEvent() {
        return new OutboxEvent(
                UUID.randomUUID(), "order-001", "Order", "OrderCreatedDomainEvent",
                "{}", "JSON", Instant.now(), null, PublishStatus.PENDING, 0, null,
                null, null, MessageNature.EVENT, null, null);
    }
}
