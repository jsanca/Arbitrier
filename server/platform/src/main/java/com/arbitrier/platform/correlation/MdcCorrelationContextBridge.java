package com.arbitrier.platform.correlation;

import com.arbitrier.platform.logging.StructuredLogFields;
import org.slf4j.MDC;

/**
 * Bridges a {@link CorrelationContext} to SLF4J MDC for structured log enrichment.
 *
 * <p>MDC is a thread-local mechanism and is appropriate for synchronous, thread-bound
 * execution models (HTTP request threads). For async execution the context must be
 * carried in log message parameters rather than MDC — see
 * {@link com.arbitrier.platform.messaging.outbox.application.DispatchOutboxMessageService}
 * for the async pattern.
 *
 * <h2>Usage</h2>
 * <pre>
 * MdcCorrelationContextBridge.bind(context);
 * try {
 *     // ... process request ...
 * } finally {
 *     MdcCorrelationContextBridge.unbind();
 * }
 * </pre>
 *
 * <p>Layer: correlation
 * <p>Module: platform
 */
public final class MdcCorrelationContextBridge {

    private MdcCorrelationContextBridge() {
    }

    /**
     * Populate MDC with the fields from the given context.
     *
     * <p>{@code requestId} is only written when non-null; non-HTTP execution models
     * that create a context without a request ID will not produce a spurious MDC entry.
     *
     * @param context the correlation context to bind; must not be null
     */
    public static void bind(final CorrelationContext context) {
        MDC.put(StructuredLogFields.CORRELATION_ID, context.correlationId());
        if (context.requestId() != null) {
            MDC.put(StructuredLogFields.REQUEST_ID, context.requestId());
        }
    }

    /**
     * Remove the correlation fields from MDC.
     *
     * <p>Must be called in the {@code finally} block of any entry point that called
     * {@link #bind(CorrelationContext)}, to prevent MDC leakage on thread pool reuse.
     */
    public static void unbind() {
        MDC.remove(StructuredLogFields.CORRELATION_ID);
        MDC.remove(StructuredLogFields.REQUEST_ID);
    }
}
