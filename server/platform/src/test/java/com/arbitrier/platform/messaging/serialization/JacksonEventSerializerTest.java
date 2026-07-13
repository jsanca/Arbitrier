package com.arbitrier.platform.messaging.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class JacksonEventSerializerTest {

    private EventSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new JacksonEventSerializer(new ObjectMapper());
    }

    @Test
    void serialize_and_deserialize_round_trip() {
        TestEvent source = new TestEvent("hello", 42);
        String payload = serializer.serialize(source);
        TestEvent restored = serializer.deserialize(payload, TestEvent.class);

        assertThat(restored.name()).isEqualTo("hello");
        assertThat(restored.value()).isEqualTo(42);
    }

    @Test
    void serialized_payload_is_json() {
        String payload = serializer.serialize(new TestEvent("x", 1));
        assertThat(payload).contains("\"name\"");
        assertThat(payload).contains("\"x\"");
        assertThat(payload).contains("\"value\"");
    }

    @Test
    void serialize_null_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> serializer.serialize(null))
                .withMessageContaining("event");
    }

    @Test
    void deserialize_blank_payload_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> serializer.deserialize("", TestEvent.class))
                .withMessageContaining("payload");
    }

    @Test
    void deserialize_null_type_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> serializer.deserialize("{}", null))
                .withMessageContaining("type");
    }

    @Test
    void constructor_rejects_null_objectMapper() {
        assertThatNullPointerException()
                .isThrownBy(() -> new JacksonEventSerializer(null))
                .withMessageContaining("objectMapper");
    }

    public record TestEvent(String name, int value) {}
}
