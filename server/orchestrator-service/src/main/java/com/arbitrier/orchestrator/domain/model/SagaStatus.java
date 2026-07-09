package com.arbitrier.orchestrator.domain.model;

/**
 * Lifecycle status for a UC-01 saga instance.
 *
 * <p>Layer: domain/model
 * <p>Module: orchestrator-service
 */
public enum SagaStatus {
    STARTED,
    AWAITING_CUSTOMER_DECISION,
    COMPENSATING,
    COMPLETED,
    CANCELLED,
    FAILED_COMPENSATION;

    /** Returns true if this saga has reached a final, unmodifiable state. */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == FAILED_COMPENSATION;
    }
}
