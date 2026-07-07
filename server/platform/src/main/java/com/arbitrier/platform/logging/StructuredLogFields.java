package com.arbitrier.platform.logging;

/**
 * MDC field name constants for structured log output.
 *
 * <p>Use these constants as keys when calling {@code MDC.put()} in adapters and services
 * so that all Arbitrier services share a consistent log schema.
 *
 * <p>Every log line produced inside a saga step must include
 * {@link #SAGA_ID}, {@link #ORDER_ID}, and {@link #TRACE_ID}.
 */
public final class StructuredLogFields {

    // ── Saga / business context ───────────────────────────────────────────────

    /** MDC key for the saga instance identifier. */
    public static final String SAGA_ID = "sagaId";

    /** MDC key for the order identifier. */
    public static final String ORDER_ID = "orderId";

    // ── Distributed tracing ───────────────────────────────────────────────────

    /** MDC key for the OpenTelemetry trace ID. */
    public static final String TRACE_ID = "traceId";

    /** MDC key for the OpenTelemetry span ID. */
    public static final String SPAN_ID = "spanId";

    // ── Message correlation ───────────────────────────────────────────────────

    /** MDC key for {@link com.arbitrier.platform.correlation.CorrelationId}. */
    public static final String CORRELATION_ID = "correlationId";

    /** MDC key for {@link com.arbitrier.platform.correlation.CausationId}. */
    public static final String CAUSATION_ID = "causationId";

    /** MDC key for {@link com.arbitrier.platform.correlation.MessageId}. */
    public static final String MESSAGE_ID = "messageId";

    /** MDC key for {@link com.arbitrier.platform.correlation.RequestId}. */
    public static final String REQUEST_ID = "requestId";

    // ── Idempotency ───────────────────────────────────────────────────────────

    /** MDC key for {@link com.arbitrier.platform.idempotency.IdempotencyKey}. */
    public static final String IDEMPOTENCY_KEY = "idempotencyKey";

    // ── Service metadata ──────────────────────────────────────────────────────

    /** MDC key for the originating service name. */
    public static final String SERVICE_NAME = "serviceName";

    /** MDC key for the event or command type name. */
    public static final String EVENT_TYPE = "eventType";

    private StructuredLogFields() {
    }
}
