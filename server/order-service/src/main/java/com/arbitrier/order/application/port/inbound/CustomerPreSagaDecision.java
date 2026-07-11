package com.arbitrier.order.application.port.inbound;

/**
 * Explicit customer decision recorded before the saga is started.
 *
 * <p>The decision is made after the customer reviews the pre-saga availability check result.
 * The saga is not started until the customer commits to one of these options:
 * <ul>
 *   <li>{@link #ACCEPT_FULL} — customer accepts the order at full requested quantities
 *       (only valid when all lines are fully available).</li>
 *   <li>{@link #ACCEPT_PARTIAL} — customer accepts only the available quantities;
 *       the saga will be submitted with available quantities only.</li>
 *   <li>{@link #CANCEL} — customer cancels before any saga is started.</li>
 * </ul>
 *
 * <p>Layer: application/port/inbound
 * <p>Module: order-service
 */
public enum CustomerPreSagaDecision {
    ACCEPT_FULL,
    ACCEPT_PARTIAL,
    CANCEL
}
