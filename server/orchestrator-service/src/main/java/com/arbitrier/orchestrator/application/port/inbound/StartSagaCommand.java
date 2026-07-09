package com.arbitrier.orchestrator.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * Command to start a new saga instance for a placed order.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: orchestrator-service
 */
public record StartSagaCommand(String sagaId, String orderId, String customerId) {

    public StartSagaCommand {
        Require.notBlank(sagaId, "StartSagaCommand.sagaId");
        Require.notBlank(orderId, "StartSagaCommand.orderId");
        Require.notBlank(customerId, "StartSagaCommand.customerId");
    }
}
