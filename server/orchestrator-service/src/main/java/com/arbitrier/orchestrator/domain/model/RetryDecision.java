package com.arbitrier.orchestrator.domain.model;

/**
 * Outcome of evaluating a retry policy against the current attempt number.
 *
 * <p>Layer: domain/model
 * <p>Module: orchestrator-service
 */
public enum RetryDecision {
    RETRY,
    EXHAUST;

    /** Returns {@code true} if the decision is to attempt another retry. */
    public boolean shouldRetry() {
        return this == RETRY;
    }
}
