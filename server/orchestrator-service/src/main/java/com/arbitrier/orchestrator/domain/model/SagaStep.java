package com.arbitrier.orchestrator.domain.model;

/**
 * The current processing step of the UC-01 saga.
 *
 * <p>Inventory is reserved before credit (RF-UC-01-002).
 *
 * <p>Layer: domain/model
 * <p>Module: orchestrator-service
 */
public enum SagaStep {
    RESERVE_INVENTORY,
    VALIDATE_CREDIT,
    AWAIT_CUSTOMER_DECISION,
    COMPLETE_ORDER,
    COMPENSATE_INVENTORY,
    COMPENSATE_CREDIT
}
