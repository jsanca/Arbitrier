package com.arbitrier.platform.kafka;

/**
 * Standard Kafka message header name constants shared across all Arbitrier services.
 *
 * <p>Header taxonomy:
 * <ul>
 *   <li>{@link #MESSAGE_ID} — unique identifier for this message (UUID string).</li>
 *   <li>{@link #CORRELATION_ID} — business-level correlation; traces the full operation across services.</li>
 *   <li>{@link #CAUSATION_ID} — ID of the upstream message that triggered this one; absent for root messages.</li>
 *   <li>{@link #TRACEPARENT} — W3C Trace Context propagation header (ADR-0008); owned by OpenTelemetry SDK.</li>
 *   <li>{@link #TRACESTATE} — W3C Trace Context vendor state; forwarded unchanged.</li>
 *   <li>{@link #SCHEMA_VERSION} — Avro schema version string (e.g. {@code "v1"}).</li>
 * </ul>
 *
 * <p><strong>B3 headers are not a platform convention.</strong> Do not add {@code X-B3-TraceId} or
 * similar headers to Kafka messages. See ADR-0008.
 *
 * <p>{@link #TRACEPARENT} and {@link #TRACESTATE} are defined here for reference only.
 * They should be populated by the OpenTelemetry SDK/instrumentation layer, not by application code.
 *
 * <p>Layer: kafka
 * <p>Module: platform
 */
public final class KafkaHeaders {

    /** Unique identifier for this message (UUID string). */
    public static final String MESSAGE_ID = "messageId";

    /**
     * Business-level correlation identifier tracing the full operation across services.
     * Sourced from the HTTP {@code X-Correlation-Id} request header or generated fresh at the saga root.
     */
    public static final String CORRELATION_ID = "correlationId";

    /** ID of the upstream message that caused this one; absent for root messages. */
    public static final String CAUSATION_ID = "causationId";

    /**
     * W3C Trace Context {@code traceparent} header.
     * Owned by the OpenTelemetry SDK — do not generate or overwrite from application code.
     */
    public static final String TRACEPARENT = "traceparent";

    /**
     * W3C Trace Context {@code tracestate} header.
     * Forwarded unchanged by OpenTelemetry instrumentation.
     */
    public static final String TRACESTATE = "tracestate";

    /** Avro schema version string (e.g. {@code "v1"}) identifying the message schema. */
    public static final String SCHEMA_VERSION = "schemaVersion";

    /** Message name used for routing (e.g. {@code "OrderCreatedDomainEvent"}, {@code "ReserveStockCommand"}). */
    public static final String EVENT_TYPE = "eventType";

    /**
     * Nature of the message: {@code "EVENT"} or {@code "COMMAND"}.
     * Corresponds to {@link com.arbitrier.platform.messaging.outbox.MessageNature}.
     */
    public static final String MESSAGE_NATURE = "messageNature";

    /** Identifier of the aggregate that produced this message. */
    public static final String AGGREGATE_ID = "aggregateId";

    /** Short type name of the aggregate (e.g. {@code "Order"}, {@code "Saga"}). */
    public static final String AGGREGATE_TYPE = "aggregateType";

    /** Payload format identifier (e.g. {@code "JSON"}). */
    public static final String PAYLOAD_FORMAT = "payloadFormat";

    private KafkaHeaders() {
    }
}
