package com.arbitrier.order.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * A single line within a {@link PrepareCorporateBulkOrderCommand}: one SKU and the quantity
 * the customer intends to order.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: order-service
 */
public record PrepareCorporateBulkOrderLineCommand(String sku, int requestedQuantity) {

    public PrepareCorporateBulkOrderLineCommand {
        Require.notBlank(sku, "PrepareCorporateBulkOrderLineCommand.sku");
        Require.isTrue(requestedQuantity > 0,
                "PrepareCorporateBulkOrderLineCommand.requestedQuantity must be positive, got: "
                        + requestedQuantity);
    }
}
