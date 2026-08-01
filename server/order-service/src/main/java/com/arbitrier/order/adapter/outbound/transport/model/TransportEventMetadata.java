package com.arbitrier.order.adapter.outbound.transport.model;

import com.arbitrier.platform.messaging.event.EventDescriptor;
import com.arbitrier.platform.validation.Require;

/**
 * Transport-layer identity of a domain event, derived from its {@link EventDescriptor}.
 *
 * <p>The relay layer uses this record to populate Kafka headers, Avro metadata, and — in future
 * ARB-024 slices — Schema Registry subject names. It deliberately does not contain topic names,
 * partition hints, or registry URLs: those are infrastructure concerns resolved by each transport
 * adapter independently.
 *
 * <p>Version encoding: the domain uses integer versions ({@code 1, 2, …}); the transport
 * uses string versions ({@code "v1", "v2", …}) to match Avro schema naming conventions.
 *
 * <p>Layer: adapter/outbound/transport/model
 * <p>Module: order-service
 */
public record TransportEventMetadata(String eventType, String eventVersion) {

    public TransportEventMetadata {
        Require.notBlank(eventType, "TransportEventMetadata.eventType");
        Require.notBlank(eventVersion, "TransportEventMetadata.eventVersion");
    }

    /**
     * Derives transport metadata from a domain event descriptor.
     *
     * <p>Converts the integer version to the string encoding used by transport adapters:
     * version {@code 1} → {@code "v1"}.
     */
    public static TransportEventMetadata from(final EventDescriptor descriptor) {
        Require.notNull(descriptor, "descriptor");
        return new TransportEventMetadata(descriptor.type(), "v" + descriptor.version());
    }
}
