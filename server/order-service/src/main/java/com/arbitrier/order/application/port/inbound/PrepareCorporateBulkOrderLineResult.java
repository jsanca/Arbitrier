package com.arbitrier.order.application.port.inbound;

/**
 * Per-line result of the pre-saga availability check.
 *
 * <p>{@code availableQuantity} reflects how much of the requested quantity can be shipped
 * immediately. It is always {@code <= requestedQuantity}. When a customer accepts partial,
 * this is the quantity to use for the subsequent {@link SubmitCorporateBulkOrderCommand}.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: order-service
 */
public record PrepareCorporateBulkOrderLineResult(
        String sku,
        int requestedQuantity,
        int availableQuantity,
        int backorderQuantity,
        boolean fullyAvailable) {
}
