package com.arbitrier.platform.messaging.outbox;

/**
 * Strategy that resolves an outbox message to its transport destination.
 *
 * <p>The returned string is used directly as the Kafka topic name by
 * {@link com.arbitrier.platform.messaging.kafka.outbound.KafkaOutboundMessagePublisher}.
 * Implementations should return the exact topic name (e.g.
 * {@code "arbitrier.order.created.v1"}).
 *
 * <p>Implementations typically inspect {@link OutboxEvent#eventType()} and
 * {@link OutboxEvent#messageNature()} to determine the correct topic.
 *
 * <p>Layer: platform/messaging/outbox
 * <p>Module: platform
 */
public interface OutboundRoutingStrategy {

    /**
     * Resolve the transport destination for the given outbox message.
     *
     * <p>The returned value is used as the Kafka topic name by the current publisher adapter.
     *
     * @param message the outbox message to route; must not be null
     * @return a non-null, non-blank topic name
     */
    String resolveDestination(OutboxEvent message);
}
