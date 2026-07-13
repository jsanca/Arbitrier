package com.arbitrier.platform.messaging.outbox;

/**
 * Runtime-neutral port for dispatching a persisted outbound message to its transport.
 *
 * <p>An outbox drainer reads {@link OutboxEvent} rows in {@link PublishStatus#PENDING} state
 * and calls this port to hand each message to the underlying transport (e.g. Kafka). The
 * transport adapter implements this interface; no Kafka, Avro, or broker-specific types appear
 * here.
 *
 * <p>The method is named {@code publish} rather than {@code send} to express intent without
 * implying point-to-point delivery. The port handles both domain events and commands
 * ({@link MessageNature}) through the same call, keeping routing decisions in the adapter.
 *
 * <p>Transport errors propagate as unchecked exceptions; the drainer is responsible for
 * calling {@link OutboxRepository#markFailed(java.util.UUID)} on failure.
 *
 * <p>Layer: platform/messaging/outbox
 * <p>Module: platform
 */
public interface OutboundMessagePublisher {

    /**
     * Publish a single persisted outbound message to the runtime transport.
     *
     * @param message the outbox message to dispatch; must not be null
     */
    void publish(OutboxEvent message);
}
