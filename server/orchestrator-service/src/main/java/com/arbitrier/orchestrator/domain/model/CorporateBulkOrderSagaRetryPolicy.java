package com.arbitrier.orchestrator.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Retry attempt configuration for the UC-01 Corporate Bulk Order saga.
 * Expresses the maximum number of retries allowed per step before compensation is triggered.
 *
 * <p>This record models retry-attempt decisions only — it carries no timeout durations.
 * Duration configuration belongs to the runtime scheduler (a future infrastructure slice).
 *
 * <p>Layer: domain/model
 * <p>Module: orchestrator-service
 */
public record CorporateBulkOrderSagaRetryPolicy(int inventoryMaxAttempts, int creditMaxAttempts) {

    public CorporateBulkOrderSagaRetryPolicy {
        Require.isTrue(inventoryMaxAttempts >= 1,
                "CorporateBulkOrderSagaRetryPolicy.inventoryMaxAttempts must be >= 1, got: "
                        + inventoryMaxAttempts);
        Require.isTrue(creditMaxAttempts >= 1,
                "CorporateBulkOrderSagaRetryPolicy.creditMaxAttempts must be >= 1, got: "
                        + creditMaxAttempts);
    }

    /** Returns {@link RetryDecision#RETRY} if more inventory attempts remain, {@link RetryDecision#EXHAUST} otherwise. */
    public RetryDecision evaluateInventory(final int attemptNumber) {
        return new RetryContext(attemptNumber, inventoryMaxAttempts).evaluate();
    }

    /** Returns {@link RetryDecision#RETRY} if more credit attempts remain, {@link RetryDecision#EXHAUST} otherwise. */
    public RetryDecision evaluateCredit(final int attemptNumber) {
        return new RetryContext(attemptNumber, creditMaxAttempts).evaluate();
    }
}
