package com.arbitrier.platform.messaging.outbox;

/**
 * Strategy that resolves an outbox message to a runtime-independent logical destination.
 *
 * <p>The logical destination is a domain-level routing identifier (e.g. {@code "order.created"},
 * {@code "credit.commands"}) with no knowledge of Kafka topics, partitions, or transport
 * details. Infrastructure adapters (e.g. a Kafka publisher) will translate the logical
 * destination to the physical address at dispatch time.
 *
 * <p>Implementations typically inspect {@link OutboxEvent#eventType()} and
 * {@link OutboxEvent#messageNature()} to determine the correct destination.
 *
 * <p>Layer: platform/messaging/outbox
 * <p>Module: platform
 */
public interface OutboundRoutingStrategy {

    /**
     * Resolve the logical destination for the given outbox message.
     *
     * @param message the outbox message to route; must not be null
     * @return a non-null, non-blank logical destination identifier
     */
    String resolveDestination(OutboxEvent message);
}
