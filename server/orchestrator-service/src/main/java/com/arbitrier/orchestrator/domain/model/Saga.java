package com.arbitrier.orchestrator.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Aggregate root for the UC-01 orchestrated saga.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@link #applyCustomerDecision(CustomerDecision)} requires
 *       {@code AWAITING_CUSTOMER_DECISION} status.</li>
 *   <li>{@link #advance(SagaStep)}, {@link #inventoryReserved(String)},
 *       {@link #creditApproved(String)}, and {@link #compensate()} require a non-terminal,
 *       non-COMPENSATING status.</li>
 *   <li>{@link #creditRejected()} additionally requires a non-null
 *       {@link #stockReservationId()} — the reservation must exist to be released.</li>
 *   <li>{@link #inventoryReleased()} requires {@code COMPENSATING} status.</li>
 *   <li>Terminal sagas ({@code COMPLETED}, {@code CANCELLED}, {@code FAILED_COMPENSATION})
 *       cannot transition further (except {@link #failCompensation()}).</li>
 * </ul>
 *
 * <p>Layer: domain/model
 * <p>Module: orchestrator-service
 */
public final class Saga {

    private final SagaId id;
    private final String orderId;
    private final String customerId;
    private final SagaStatus status;
    private final SagaStep currentStep;
    private final CustomerDecision customerDecision; // null until decision is applied
    private final String stockReservationId;         // null until inventory confirms reservation
    private final String creditReservationId;        // null until credit confirms approval
    /** Opaque optimistic-lock token set by the persistence adapter; null for new sagas. */
    private final Long version;

    private Saga(SagaId id, String orderId, String customerId, SagaStatus status,
                 SagaStep currentStep, CustomerDecision customerDecision,
                 String stockReservationId, String creditReservationId, Long version) {
        this.id = Require.notNull(id, "Saga.id");
        this.orderId = Require.notBlank(orderId, "Saga.orderId");
        this.customerId = Require.notBlank(customerId, "Saga.customerId");
        this.status = Require.notNull(status, "Saga.status");
        this.currentStep = Require.notNull(currentStep, "Saga.currentStep");
        this.customerDecision = customerDecision;
        this.stockReservationId = stockReservationId;
        this.creditReservationId = creditReservationId;
        this.version = version;
    }

    /** Starts a new saga at the {@code ORDER_CREATED} step. */
    public static Saga start(SagaId id, String orderId, String customerId) {
        return new Saga(id, orderId, customerId, SagaStatus.STARTED, SagaStep.ORDER_CREATED,
                null, null, null, null);
    }

    /**
     * Rehydrates a saga from a persistent store.
     *
     * <p>The {@code version} token must match what the adapter read from the database; it
     * is used to detect concurrent modifications when the saga is later saved.
     */
    public static Saga reconstruct(SagaId id, String orderId, String customerId,
                                   SagaStatus status, SagaStep currentStep,
                                   CustomerDecision customerDecision,
                                   String stockReservationId, String creditReservationId,
                                   Long version) {
        return new Saga(id, orderId, customerId, status, currentStep,
                customerDecision, stockReservationId, creditReservationId, version);
    }

    /**
     * Records that inventory has been reserved and advances to the {@code VALIDATE_CREDIT} step.
     *
     * <p>The {@code stockReservationId} is stored on the saga for future compensation (ARB-016).
     *
     * @throws NullPointerException     if {@code stockReservationId} is null
     * @throws IllegalArgumentException if {@code stockReservationId} is blank
     * @throws IllegalArgumentException if the saga is in a terminal state
     */
    public Saga inventoryReserved(String stockReservationId) {
        Require.notBlank(stockReservationId, "stockReservationId");
        Require.isTrue(!status.isTerminal(),
                "inventoryReserved() requires non-terminal status, current: " + status);
        return new Saga(id, orderId, customerId, status, SagaStep.VALIDATE_CREDIT,
                customerDecision, stockReservationId, creditReservationId, version);
    }

    /**
     * Records that credit has been approved.
     *
     * <p>The {@code creditReservationId} is stored on the saga for future compensation (ARB-016).
     * Call {@link #complete()} immediately after to finalise the saga.
     *
     * @throws NullPointerException     if {@code creditReservationId} is null
     * @throws IllegalArgumentException if {@code creditReservationId} is blank
     * @throws IllegalArgumentException if the saga is in a terminal state
     */
    public Saga creditApproved(String creditReservationId) {
        Require.notBlank(creditReservationId, "creditReservationId");
        Require.isTrue(!status.isTerminal(),
                "creditApproved() requires non-terminal status, current: " + status);
        return new Saga(id, orderId, customerId, status, currentStep,
                customerDecision, stockReservationId, creditReservationId, version);
    }

    /**
     * Cancels the saga directly because stock was rejected.
     *
     * <p>No inventory was reserved so no compensation command is needed.
     *
     * @throws IllegalArgumentException if the saga is already in a terminal state
     */
    public Saga stockRejected() {
        Require.isTrue(!status.isTerminal(),
                "stockRejected() requires non-terminal status, current: " + status);
        return new Saga(id, orderId, customerId, SagaStatus.CANCELLED, currentStep,
                customerDecision, stockReservationId, creditReservationId, version);
    }

    /**
     * Begins compensation because credit was rejected after inventory was reserved.
     *
     * <p>Transitions to {@code COMPENSATING} status and advances step to
     * {@code COMPENSATE_INVENTORY}. The stored {@link #stockReservationId()} must be
     * non-null so that the application service can issue a ReleaseStock command.
     *
     * @throws IllegalArgumentException if the saga is already in a terminal state
     * @throws IllegalArgumentException if {@link #stockReservationId()} is null
     */
    public Saga creditRejected() {
        Require.isTrue(!status.isTerminal(),
                "creditRejected() requires non-terminal status, current: " + status);
        Require.isTrue(stockReservationId != null,
                "creditRejected() requires a stock reservation to compensate, sagaId: " + id);
        return new Saga(id, orderId, customerId, SagaStatus.COMPENSATING,
                SagaStep.COMPENSATE_INVENTORY, customerDecision, stockReservationId, creditReservationId, version);
    }

    /**
     * Cancels the saga after stock compensation has completed.
     *
     * <p>Only valid while {@code COMPENSATING} — the compensation flow for inventory
     * has finished and the saga can now reach the {@code CANCELLED} terminal state.
     *
     * @throws IllegalArgumentException if the saga is not in {@code COMPENSATING} status
     */
    public Saga inventoryReleased() {
        Require.isTrue(status == SagaStatus.COMPENSATING,
                "inventoryReleased() requires COMPENSATING status, current: " + status);
        return new Saga(id, orderId, customerId, SagaStatus.CANCELLED, currentStep,
                customerDecision, stockReservationId, creditReservationId, version);
    }

    /** Transitions to {@code AWAITING_CUSTOMER_DECISION} after partial inventory. */
    public Saga awaitCustomerDecision() {
        Require.isTrue(status == SagaStatus.STARTED,
                "awaitCustomerDecision() requires STARTED status, current: " + status);
        return new Saga(id, orderId, customerId, SagaStatus.AWAITING_CUSTOMER_DECISION,
                SagaStep.AWAIT_CUSTOMER_DECISION, null, stockReservationId, creditReservationId, version);
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
        return new Saga(id, orderId, customerId, SagaStatus.STARTED, SagaStep.VALIDATE_CREDIT,
                decision, stockReservationId, creditReservationId, version);
    }

    /**
     * Advances the saga to the given step without changing its status.
     *
     * <p>Used by {@code AdvanceSagaUseCase} for general step progression. Does not execute
     * any business logic — the caller is responsible for selecting the correct next step.
     *
     * @throws NullPointerException     if {@code nextStep} is null
     * @throws IllegalArgumentException if the saga is terminal or COMPENSATING
     */
    public Saga advance(SagaStep nextStep) {
        Require.notNull(nextStep, "nextStep");
        Require.isTrue(!status.isTerminal(),
                "advance() requires non-terminal status, current: " + status);
        Require.isTrue(status != SagaStatus.COMPENSATING,
                "advance() is not valid while COMPENSATING");
        return new Saga(id, orderId, customerId, status, nextStep,
                customerDecision, stockReservationId, creditReservationId, version);
    }

    /**
     * Begins compensation by transitioning status to {@code COMPENSATING}.
     *
     * <p>Does not execute any compensating commands — that wiring belongs to ARB-016.
     *
     * @throws IllegalArgumentException if the saga is already terminal or COMPENSATING
     */
    public Saga compensate() {
        Require.isTrue(!status.isTerminal(),
                "compensate() requires non-terminal status, current: " + status);
        Require.isTrue(status != SagaStatus.COMPENSATING,
                "Saga is already COMPENSATING");
        return new Saga(id, orderId, customerId, SagaStatus.COMPENSATING, currentStep,
                customerDecision, stockReservationId, creditReservationId, version);
    }

    /**
     * Transitions the saga to {@code COMPLETED}.
     *
     * @throws IllegalArgumentException if the saga is already in a terminal state
     */
    public Saga complete() {
        Require.isTrue(!status.isTerminal(),
                "complete() requires non-terminal status, current: " + status);
        return new Saga(id, orderId, customerId, SagaStatus.COMPLETED, SagaStep.COMPLETE_ORDER,
                customerDecision, stockReservationId, creditReservationId, version);
    }

    /**
     * Transitions the saga to {@code CANCELLED}.
     *
     * @throws IllegalArgumentException if the saga is already in a terminal state
     */
    public Saga cancel() {
        Require.isTrue(!status.isTerminal(),
                "cancel() requires non-terminal status, current: " + status);
        return new Saga(id, orderId, customerId, SagaStatus.CANCELLED, currentStep,
                customerDecision, stockReservationId, creditReservationId, version);
    }

    /**
     * Begins inventory compensation by moving to the {@code COMPENSATE_INVENTORY} step.
     *
     * @throws IllegalArgumentException if the saga is already in a terminal state
     */
    public Saga compensateInventory() {
        Require.isTrue(!status.isTerminal(),
                "compensateInventory() requires non-terminal status, current: " + status);
        return new Saga(id, orderId, customerId, status, SagaStep.COMPENSATE_INVENTORY,
                customerDecision, stockReservationId, creditReservationId, version);
    }

    /**
     * Begins credit compensation by moving to the {@code COMPENSATE_CREDIT} step.
     *
     * @throws IllegalArgumentException if the saga is already in a terminal state
     */
    public Saga compensateCredit() {
        Require.isTrue(!status.isTerminal(),
                "compensateCredit() requires non-terminal status, current: " + status);
        return new Saga(id, orderId, customerId, status, SagaStep.COMPENSATE_CREDIT,
                customerDecision, stockReservationId, creditReservationId, version);
    }

    /** Transitions the saga to {@code FAILED_COMPENSATION}. */
    public Saga failCompensation() {
        return new Saga(id, orderId, customerId, SagaStatus.FAILED_COMPENSATION, currentStep,
                customerDecision, stockReservationId, creditReservationId, version);
    }

    // ── Timeout transitions (ARB-018) ─────────────────────────────────────────

    /**
     * Transitions to {@code WAITING_FOR_INVENTORY} after issuing a ReserveStock command.
     *
     * @throws IllegalArgumentException if the saga is not in {@code STARTED} status
     */
    public Saga awaitInventoryResponse() {
        Require.isTrue(status == SagaStatus.STARTED,
                "awaitInventoryResponse() requires STARTED status, current: " + status);
        return new Saga(id, orderId, customerId, SagaStatus.WAITING_FOR_INVENTORY,
                SagaStep.RESERVE_INVENTORY, customerDecision, stockReservationId, creditReservationId, version);
    }

    /**
     * Transitions to {@code WAITING_FOR_CREDIT} after issuing a ReserveCredit command.
     *
     * @throws IllegalArgumentException if the saga is not in {@code WAITING_FOR_INVENTORY} status
     */
    public Saga awaitCreditResponse() {
        Require.isTrue(status == SagaStatus.WAITING_FOR_INVENTORY,
                "awaitCreditResponse() requires WAITING_FOR_INVENTORY status, current: " + status);
        return new Saga(id, orderId, customerId, SagaStatus.WAITING_FOR_CREDIT,
                currentStep, customerDecision, stockReservationId, creditReservationId, version);
    }

    /**
     * Validates that the saga is awaiting an inventory response — confirms the timeout is legal.
     *
     * <p>Returns {@code this}; the result should be used to chain further transitions.
     *
     * @throws IllegalArgumentException if the saga is not in {@code WAITING_FOR_INVENTORY} status
     */
    public Saga inventoryTimedOut() {
        Require.isTrue(status == SagaStatus.WAITING_FOR_INVENTORY,
                "inventoryTimedOut() requires WAITING_FOR_INVENTORY status, current: " + status);
        return this;
    }

    /**
     * Validates that the saga is awaiting a credit response — confirms the timeout is legal.
     *
     * <p>Returns {@code this}; the result should be used to chain further transitions.
     *
     * @throws IllegalArgumentException if the saga is not in {@code WAITING_FOR_CREDIT} status
     */
    public Saga creditTimedOut() {
        Require.isTrue(status == SagaStatus.WAITING_FOR_CREDIT,
                "creditTimedOut() requires WAITING_FOR_CREDIT status, current: " + status);
        return this;
    }

    /**
     * Records that the inventory reservation is being retried; saga remains in
     * {@code WAITING_FOR_INVENTORY}.
     *
     * @throws IllegalArgumentException if the saga is not in {@code WAITING_FOR_INVENTORY} status
     */
    public Saga retryInventory() {
        Require.isTrue(status == SagaStatus.WAITING_FOR_INVENTORY,
                "retryInventory() requires WAITING_FOR_INVENTORY status, current: " + status);
        return this;
    }

    /**
     * Records that the credit reservation is being retried; saga remains in
     * {@code WAITING_FOR_CREDIT}.
     *
     * @throws IllegalArgumentException if the saga is not in {@code WAITING_FOR_CREDIT} status
     */
    public Saga retryCredit() {
        Require.isTrue(status == SagaStatus.WAITING_FOR_CREDIT,
                "retryCredit() requires WAITING_FOR_CREDIT status, current: " + status);
        return this;
    }

    /** Returns the unique saga identifier. */
    public SagaId id() {
        return id;
    }

    /** Returns the identifier of the order this saga orchestrates. */
    public String orderId() {
        return orderId;
    }

    /** Returns the customer identifier associated with this saga. */
    public String customerId() {
        return customerId;
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

    /** Returns the stock reservation identifier, or {@code null} before inventory is reserved. */
    public String stockReservationId() {
        return stockReservationId;
    }

    /** Returns the credit reservation identifier, or {@code null} before credit is approved. */
    public String creditReservationId() {
        return creditReservationId;
    }

    /** Returns the opaque optimistic-lock token, or {@code null} for new (never-persisted) sagas. */
    public Long version() {
        return version;
    }
}
