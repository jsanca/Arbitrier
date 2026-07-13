package com.arbitrier.platform.messaging.serialization;

/**
 * Contract for serializing and deserializing domain events to/from a string payload.
 *
 * <p>Concrete implementations decide the wire format (JSON, Avro-JSON, etc.).
 * The serializer is used by the outbox pipeline to encode events for durable storage
 * before they are picked up by a publisher.
 *
 * <p>Layer: platform/messaging/serialization
 * <p>Module: platform
 */
public interface EventSerializer {

    /**
     * Serialize the given event object to a string payload.
     *
     * @param event the event to serialize; must not be null
     * @return the serialized string payload
     */
    String serialize(Object event);

    /**
     * Deserialize the given string payload to an instance of {@code type}.
     *
     * @param payload the serialized payload; must not be null or blank
     * @param type    the target type; must not be null
     * @param <T>     the target type
     * @return the deserialized object
     */
    <T> T deserialize(String payload, Class<T> type);
}
