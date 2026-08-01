package com.arbitrier.platform.messaging.event;

import com.arbitrier.platform.validation.Require;

/**
 * Stable logical identity of a domain event, independent of Java class names, Avro schemas,
 * Kafka topics, and Schema Registry subjects.
 *
 * <p>{@code type} is a dot-separated logical name that must remain stable across refactors —
 * e.g. {@code "order.created"}. It becomes the input for Schema Registry subject naming and
 * Kafka topic routing without coupling those concerns to the Java type hierarchy.
 *
 * <p>{@code version} is a monotonically increasing integer (1, 2, …). Transport adapters convert
 * this to their own encoding — e.g. {@code "v1"} for Avro schema versions or Kafka header values.
 *
 * <p>Layer: platform/messaging/event
 * <p>Module: platform
 */
public record EventDescriptor(String type, int version) {

    public EventDescriptor {
        Require.notBlank(type, "EventDescriptor.type");
        if (version < 1) {
            throw new IllegalArgumentException("EventDescriptor.version must be >= 1, got: " + version);
        }
    }
}
