package com.arbitrier.order.application.port.inbound;

import java.util.List;

/**
 * Result of the pre-saga availability negotiation.
 *
 * <h2>Line groupings</h2>
 * <ul>
 *   <li>{@link #requestedLines} — all lines at their originally requested quantities.</li>
 *   <li>{@link #availableLines} — subset where {@code availableQuantity > 0}; these are the
 *       lines a customer can receive immediately. If the customer chooses
 *       {@link CustomerPreSagaDecision#ACCEPT_PARTIAL}, only these lines (at their
 *       {@code availableQuantity}) should be passed to
 *       {@link SubmitCorporateBulkOrderCommand}.</li>
 *   <li>{@link #backorderLines} — subset where {@code backorderQuantity > 0}; these lines
 *       cannot be fully fulfilled immediately. A line may appear in both
 *       {@code availableLines} and {@code backorderLines} when it is partially available.</li>
 * </ul>
 *
 * <h2>Advisory nature</h2>
 * <p>This result is non-binding. Stock levels may change between the pre-check and the
 * actual reservation attempt inside the saga. If the real reservation later fails, existing
 * saga compensation paths handle the rollback.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: order-service
 */
public record PrepareCorporateBulkOrderResult(
        boolean allAvailable,
        List<PrepareCorporateBulkOrderLineResult> requestedLines,
        List<PrepareCorporateBulkOrderLineResult> availableLines,
        List<PrepareCorporateBulkOrderLineResult> backorderLines,
        RecommendedAction recommendedAction) {

    public PrepareCorporateBulkOrderResult {
        requestedLines = List.copyOf(requestedLines);
        availableLines = List.copyOf(availableLines);
        backorderLines = List.copyOf(backorderLines);
    }
}
