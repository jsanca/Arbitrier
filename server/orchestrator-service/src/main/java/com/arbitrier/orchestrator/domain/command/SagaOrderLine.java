package com.arbitrier.orchestrator.domain.command;

import com.arbitrier.platform.validation.Require;

/**
 * A single order line carried by saga commands that propagate item quantities to participant
 * services such as the inventory-service stock reservation command.
 *
 * <p>Layer: domain/command
 * <p>Module: orchestrator-service
 */
public record SagaOrderLine(String sku, int quantity) {

    public SagaOrderLine {
        Require.notBlank(sku, "SagaOrderLine.sku");
        Require.isTrue(quantity > 0, "SagaOrderLine.quantity must be positive, got: " + quantity);
    }
}
