package com.arbitrier.platform.messaging.event;

/**
 * Marker interface for domain events that carry a stable, transport-independent logical identity.
 *
 * <p>Implementing classes declare their {@link EventDescriptor} — a fixed pair of logical type
 * (e.g. {@code "order.created"}) and version (e.g. {@code 1}) — which the Outbox mapper and
 * transport adapters use instead of deriving identity from the Java class name.
 *
 * <p>The contract is intentionally minimal: the domain carries only what it knows about itself.
 * Topic names, Schema Registry subjects, and Kafka header values are derived by the transport
 * layer from the descriptor.
 *
 * <p>Layer: platform/messaging/event
 * <p>Module: platform
 */
public interface DomainEvent {

    /** Returns the stable logical identity of this event type. */
    EventDescriptor descriptor();
}
