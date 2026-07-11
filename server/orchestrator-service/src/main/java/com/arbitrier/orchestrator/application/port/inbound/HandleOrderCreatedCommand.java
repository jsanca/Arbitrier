package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.orchestrator.domain.command.SagaOrderLine;
import com.arbitrier.platform.validation.Require;

import java.util.List;

/**
 * Command to handle the OrderCreated event and start a new saga instance.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleOrderCreatedCommand(
        String sagaId,
        String orderId,
        String customerId,
        List<SagaOrderLine> lines) {

    public HandleOrderCreatedCommand {
        Require.notBlank(sagaId, "HandleOrderCreatedCommand.sagaId");
        Require.notBlank(orderId, "HandleOrderCreatedCommand.orderId");
        Require.notBlank(customerId, "HandleOrderCreatedCommand.customerId");
        Require.notEmpty(lines, "HandleOrderCreatedCommand.lines");
        lines = List.copyOf(lines);
    }
}
