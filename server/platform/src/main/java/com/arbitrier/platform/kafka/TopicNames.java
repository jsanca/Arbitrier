package com.arbitrier.platform.kafka;

/**
 * Canonical Kafka topic name constants for all Arbitrier services.
 *
 * <p>Topic naming convention: {@code arbitrier.<domain>.<event>.v<N>}
 *
 * <p>Only {@link #ORDER_CREATED_V1} has a producer implementation in ARB-011.
 * The remaining topics are defined as reference constants for future consumer and producer tasks.
 * No consumers exist yet.
 *
 * <p>Layer: kafka
 * <p>Module: platform
 */
public final class TopicNames {

    // ── Order topics ──────────────────────────────────────────────────────────

    /** Topic for {@code OrderCreated} events published by order-service. */
    public static final String ORDER_CREATED_V1 = "arbitrier.order.created.v1";

    // ── Inventory topics (placeholders — no producers or consumers yet) ───────

    /** Topic for {@code StockReserved} events published by inventory-service. */
    public static final String STOCK_RESERVED_V1 = "arbitrier.stock.reserved.v1";

    /** Topic for {@code StockPartiallyReserved} events published by inventory-service. */
    public static final String STOCK_PARTIALLY_RESERVED_V1 = "arbitrier.stock.partially-reserved.v1";

    /** Topic for {@code StockRejected} events published by inventory-service. */
    public static final String STOCK_REJECTED_V1 = "arbitrier.stock.rejected.v1";

    // ── Credit topics (placeholders — no producers or consumers yet) ──────────

    /** Topic for {@code CreditApproved} events published by credit-service. */
    public static final String CREDIT_APPROVED_V1 = "arbitrier.credit.approved.v1";

    /** Topic for {@code CreditRejected} events published by credit-service. */
    public static final String CREDIT_REJECTED_V1 = "arbitrier.credit.rejected.v1";

    private TopicNames() {
    }
}
