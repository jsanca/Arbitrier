package com.arbitrier.orchestrator.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Carries the current attempt number and the maximum allowed attempts for a retryable saga step.
 * Call {@link #evaluate()} to obtain a {@link RetryDecision}.
 *
 * <p>Layer: domain/model
 * <p>Module: orchestrator-service
 */
public record RetryContext(int attemptNumber, int maxAttempts) {

    public RetryContext {
        Require.isTrue(attemptNumber >= 1,
                "RetryContext.attemptNumber must be >= 1, got: " + attemptNumber);
        Require.isTrue(maxAttempts >= 1,
                "RetryContext.maxAttempts must be >= 1, got: " + maxAttempts);
    }

    /**
     * Returns {@link RetryDecision#RETRY} if more attempts remain,
     * {@link RetryDecision#EXHAUST} once the last attempt has been reached.
     */
    public RetryDecision evaluate() {
        return attemptNumber < maxAttempts ? RetryDecision.RETRY : RetryDecision.EXHAUST;
    }
}
