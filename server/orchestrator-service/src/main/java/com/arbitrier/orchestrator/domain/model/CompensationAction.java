package com.arbitrier.orchestrator.domain.model;

/**
 * Compensation action to execute when a saga step needs to be rolled back.
 *
 * <p>Layer: domain/model
 * <p>Module: orchestrator-service
 */
public enum CompensationAction {
    RELEASE_INVENTORY_RESERVATION,
    RELEASE_CREDIT_RESERVATION,
    NONE
}
