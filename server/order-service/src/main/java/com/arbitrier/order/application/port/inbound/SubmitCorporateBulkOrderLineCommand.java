package com.arbitrier.order.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * A single line within a {@link SubmitCorporateBulkOrderCommand}.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: order-service
 */
public record SubmitCorporateBulkOrderLineCommand(String sku, int quantity) {

    public SubmitCorporateBulkOrderLineCommand {
        Require.notBlank(sku, "sku");
        Require.isTrue(quantity > 0, "quantity must be positive, got: " + quantity);
    }
}
