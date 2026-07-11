package com.arbitrier.order.application.port.inbound;

import com.arbitrier.platform.validation.Require;

import java.util.List;

/**
 * Application command for the pre-saga availability negotiation step.
 *
 * <p>The caller provides intended order lines. The use case checks global inventory
 * availability and returns a recommended action without creating an Order or starting a saga.
 * Warehouse selection is internal to the Inventory bounded context (ADR-0009).
 *
 * <p>Layer: application/port/inbound
 * <p>Module: order-service
 */
public record PrepareCorporateBulkOrderCommand(
        String customerId,
        String submittedByUserId,
        List<PrepareCorporateBulkOrderLineCommand> lines) {

    public PrepareCorporateBulkOrderCommand {
        Require.notBlank(customerId, "PrepareCorporateBulkOrderCommand.customerId");
        Require.notBlank(submittedByUserId, "PrepareCorporateBulkOrderCommand.submittedByUserId");
        Require.notEmpty(lines, "PrepareCorporateBulkOrderCommand.lines");
        lines = List.copyOf(lines);
    }
}
