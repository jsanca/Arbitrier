package com.arbitrier.orchestrator.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Aggregate root for the UC-01 orchestrated saga.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@link #applyCustomerDecision(CustomerDecision)} requires
 *       {@code AWAITING_CUSTOMER_DECISION} status.</li>
 *   <li>Terminal sagas ({@code COMPLETED}, {@code CANCELLED}, {@code FAILED_COMPENSATION})
 *       cannot transition further.</li>
 * </ul>
 *
 * <p>Layer: domain/model
 * <p>Module: orchestrator-service
 */
public final class Saga {

    private final SagaId id;
    private final String orderId;
    private final SagaStatus status;
    private final SagaStep currentStep;
    private final CustomerDecision customerDecision; // null until decision is applied

    private Saga(SagaId id, String orderId, SagaStatus status,
                 SagaStep currentStep, CustomerDecision customerDecision) {
        this.id = Require.notNull(id, "Saga.id");
        this.orderId = Require.notBlank(orderId, "Saga.orderId");
        this.status = Require.notNull(status, "Saga.status");
        this.currentStep = Require.notNull(currentStep, "Saga.currentStep");
        this.customerDecision = customerDecision;
    }

    /** Starts a new saga at the inventory reservation step. */
    public static Saga start(SagaId id, String orderId) {
        return new Saga(id, orderId, SagaStatus.STARTED, SagaStep.RESERVE_INVENTORY, null);
    }

    /** Transitions to {@code AWAITING_CUSTOMER_DECISION} after partial inventory. */
    public Saga awaitCustomerDecision() {
        Require.isTrue(status == SagaStatus.STARTED,
                "awaitCustomerDecision() requires STARTED status, current: " + status);
        return new Saga(id, orderId, SagaStatus.AWAITING_CUSTOMER_DECISION,
                SagaStep.AWAIT_CUSTOMER_DECISION, null);
    }

    /**
     * Records the customer decision and resumes the saga.
     * Only valid when status is {@code AWAITING_CUSTOMER_DECISION}.
     *
     * @throws NullPointerException     if {@code decision} is null
     * @throws IllegalArgumentException if the saga is not in {@code AWAITING_CUSTOMER_DECISION}
     */
    public Saga applyCustomerDecision(CustomerDecision decision) {
        Require.notNull(decision, "CustomerDecision");
        Require.isTrue(status == SagaStatus.AWAITING_CUSTOMER_DECISION,
                "applyCustomerDecision() requires AWAITING_CUSTOMER_DECISION status, current: " + status);
        return new Saga(id, orderId, SagaStatus.STARTED, SagaStep.VALIDATE_CREDIT, decision);
    }

    /**
     * Transitions the saga to {@code COMPLETED}.
     *
     * @throws IllegalArgumentException if the saga is already in a terminal state
     */
    public Saga complete() {
        Require.isTrue(!status.isTerminal(),
                "complete() requires non-terminal status, current: " + status);
        return new Saga(id, orderId, SagaStatus.COMPLETED, SagaStep.COMPLETE_ORDER,
                customerDecision);
    }

    /**
     * Transitions the saga to {@code CANCELLED}.
     *
     * @throws IllegalArgumentException if the saga is already in a terminal state
     */
    public Saga cancel() {
        Require.isTrue(!status.isTerminal(),
                "cancel() requires non-terminal status, current: " + status);
        return new Saga(id, orderId, SagaStatus.CANCELLED, currentStep, customerDecision);
    }

    /**
     * Begins inventory compensation by moving to the {@code COMPENSATE_INVENTORY} step.
     *
     * @throws IllegalArgumentException if the saga is already in a terminal state
     */
    public Saga compensateInventory() {
        Require.isTrue(!status.isTerminal(),
                "compensateInventory() requires non-terminal status, current: " + status);
        return new Saga(id, orderId, status, SagaStep.COMPENSATE_INVENTORY, customerDecision);
    }

    /**
     * Begins credit compensation by moving to the {@code COMPENSATE_CREDIT} step.
     *
     * @throws IllegalArgumentException if the saga is already in a terminal state
     */
    public Saga compensateCredit() {
        Require.isTrue(!status.isTerminal(),
                "compensateCredit() requires non-terminal status, current: " + status);
        return new Saga(id, orderId, status, SagaStep.COMPENSATE_CREDIT, customerDecision);
    }

    /** Transitions the saga to {@code FAILED_COMPENSATION}. */
    public Saga failCompensation() {
        return new Saga(id, orderId, SagaStatus.FAILED_COMPENSATION, currentStep,
                customerDecision);
    }

    /** Returns the unique saga identifier. */
    public SagaId id() {
        return id;
    }

    /** Returns the identifier of the order this saga orchestrates. */
    public String orderId() {
        return orderId;
    }

    /** Returns the current lifecycle status of this saga. */
    public SagaStatus status() {
        return status;
    }

    /** Returns the current processing step of this saga. */
    public SagaStep currentStep() {
        return currentStep;
    }

    /** Returns the customer decision, or {@code null} if no decision has been applied. */
    public CustomerDecision customerDecision() {
        return customerDecision;
    }
}
