package com.arbitrier.platform.correlation;

/**
 * Transport-independent representation of a correlation context for a single unit of work.
 *
 * <p>Decouples business code and platform infrastructure from the specific mechanism used to
 * carry correlation information (HTTP filter, MDC, thread-local, ScopeValue, etc.).
 *
 * <p>Implementations are expected to be immutable.
 *
 * <p>Layer: correlation
 * <p>Module: platform
 */
public interface CorrelationContext {

    /**
     * The correlation identifier that links related messages across service boundaries.
     *
     * @return a non-blank correlation identifier; never null
     */
    String correlationId();

    /**
     * The per-request identifier, present only in HTTP-initiated execution models.
     *
     * @return a non-blank request identifier, or {@code null} when the execution context
     *         has no request (e.g., scheduler, Kafka consumer, outbox dispatcher)
     */
    String requestId();
}
