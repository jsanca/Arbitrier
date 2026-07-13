package com.arbitrier.platform.messaging.outbox;

/**
 * Strategy that serializes an outbox message into a transport payload.
 *
 * <p>The abstraction is transport-neutral: it does not expose Kafka, Avro, or
 * any other infrastructure types. The returned {@code String} is passed as-is
 * to the active {@link OutboundMessagePublisher} adapter.
 *
 * <p>Implementations decide the wire format (JSON, Avro-JSON, etc.). The
 * current production implementation ({@code JsonOutboundPayloadSerializer})
 * returns the pre-serialized {@link OutboxEvent#payload()} unchanged. An Avro
 * implementation would re-encode the payload as the Avro wire format.
 *
 * <p>Layer: platform/messaging/outbox
 * <p>Module: platform
 */
public interface OutboundPayloadSerializer {

    /**
     * Serialize the given outbox message into a transport payload.
     *
     * @param event the outbox message to serialize; must not be null
     * @return the serialized payload; never null
     */
    String serialize(OutboxEvent event);
}
