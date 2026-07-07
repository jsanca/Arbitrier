package com.arbitrier.platform.observability;

/**
 * Canonical observation (span) name constants for OpenTelemetry instrumentation.
 *
 * <p>Use these as the {@code name} argument when creating Micrometer Observations or
 * OpenTelemetry spans so that all services share a consistent telemetry naming scheme.
 *
 * <p>Convention: {@code <service>.<operation>} using kebab-case.
 */
public final class ObservationNames {

    // ── Order Service ─────────────────────────────────────────────────────────

    /** Span name for placing a bulk order. */
    public static final String ORDER_PLACE_BULK_ORDER = "order-service.place-bulk-order";

    // ── Credit Service ────────────────────────────────────────────────────────

    /** Span name for reserving B2B credit. */
    public static final String CREDIT_RESERVE_CREDIT = "credit-service.reserve-credit";

    /** Span name for releasing B2B credit (compensation). */
    public static final String CREDIT_RELEASE_CREDIT = "credit-service.release-credit";

    // ── Inventory Service ─────────────────────────────────────────────────────

    /** Span name for reserving inventory stock. */
    public static final String INVENTORY_RESERVE_STOCK = "inventory-service.reserve-stock";

    /** Span name for releasing reserved inventory (compensation). */
    public static final String INVENTORY_RELEASE_STOCK = "inventory-service.release-stock";

    // ── Orchestrator Service ──────────────────────────────────────────────────

    /** Span name for starting the UC-01 saga. */
    public static final String ORCHESTRATOR_SAGA_START = "orchestrator-service.saga-start";

    /** Span name for a single saga state transition step. */
    public static final String ORCHESTRATOR_SAGA_STEP = "orchestrator-service.saga-step";

    private ObservationNames() {
    }
}
