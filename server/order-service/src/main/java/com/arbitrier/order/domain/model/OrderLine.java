package com.arbitrier.order.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * A single line in an order, pairing a SKU with a requested quantity.
 *
 * <p>Layer: domain/model
 * <p>Module: order-service
 */
public record OrderLine(Sku sku, Quantity quantity) {
    public OrderLine {
        Require.notNull(sku, "OrderLine.sku");
        Require.notNull(quantity, "OrderLine.quantity");
    }
}
