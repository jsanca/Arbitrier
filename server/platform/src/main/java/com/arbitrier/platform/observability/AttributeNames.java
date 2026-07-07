package com.arbitrier.platform.observability;

/**
 * OpenTelemetry span attribute name constants.
 *
 * <p>Use these as attribute keys on spans to ensure consistent telemetry across services.
 * Convention: {@code arbitrier.<domain>.<field>} using dot-separated lowercase.
 */
public final class AttributeNames {

    // ── Saga / business context ───────────────────────────────────────────────

    /** Span attribute for the saga instance identifier. */
    public static final String SAGA_ID = "arbitrier.saga.id";

    /** Span attribute for the order identifier. */
    public static final String ORDER_ID = "arbitrier.order.id";

    /** Span attribute for the final saga outcome state. */
    public static final String SAGA_OUTCOME = "arbitrier.saga.outcome";

    // ── Message correlation ───────────────────────────────────────────────────

    /** Span attribute for the correlation identifier. */
    public static final String CORRELATION_ID = "arbitrier.correlation.id";

    /** Span attribute for the causation identifier. */
    public static final String CAUSATION_ID = "arbitrier.causation.id";

    /** Span attribute for the message identifier. */
    public static final String MESSAGE_ID = "arbitrier.message.id";

    // ── Idempotency ───────────────────────────────────────────────────────────

    /** Span attribute for the idempotency key. */
    public static final String IDEMPOTENCY_KEY = "arbitrier.idempotency.key";

    /** Span attribute indicating whether the request was a detected duplicate. */
    public static final String IDEMPOTENCY_DUPLICATE = "arbitrier.idempotency.duplicate";

    // ── Service metadata ──────────────────────────────────────────────────────

    /** Span attribute for the originating service name. */
    public static final String SERVICE_NAME = "arbitrier.service.name";

    private AttributeNames() {
    }
}
