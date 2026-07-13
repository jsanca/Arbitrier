package com.arbitrier.platform.messaging.serialization;

import com.arbitrier.platform.validation.Require;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson-based {@link EventSerializer} implementation.
 *
 * <p>Delegates to an injected {@link ObjectMapper} — services may configure custom
 * modules (JavaTime, JSpecify, etc.) at bean-registration time.
 *
 * <p>Layer: platform/messaging/serialization
 * <p>Module: platform
 */
public final class JacksonEventSerializer implements EventSerializer {

    private final ObjectMapper objectMapper;

    public JacksonEventSerializer(final ObjectMapper objectMapper) {
        this.objectMapper = Require.notNull(objectMapper, "objectMapper");
    }

    @Override
    public String serialize(final Object event) {

        Require.notNull(event, "event");
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Failed to serialize event of type " + event.getClass().getName(), e);
        }
    }

    @Override
    public <T> T deserialize(final String payload, final Class<T> type) {

        Require.notBlank(payload, "payload");
        Require.notNull(type, "type");
        try {
            return objectMapper.readValue(payload, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Failed to deserialize payload to type " + type.getName(), e);
        }
    }
}
