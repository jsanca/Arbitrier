package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * Command to handle the OrderCreated event and start a new saga instance.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record HandleOrderCreatedCommand(String sagaId, String orderId, String customerId) {

    public HandleOrderCreatedCommand {
        Require.notBlank(sagaId, "HandleOrderCreatedCommand.sagaId");
        Require.notBlank(orderId, "HandleOrderCreatedCommand.orderId");
        Require.notBlank(customerId, "HandleOrderCreatedCommand.customerId");
    }
}
