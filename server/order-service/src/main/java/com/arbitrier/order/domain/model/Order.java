package com.arbitrier.order.domain.model;

import com.arbitrier.platform.validation.Require;
import java.util.List;

/**
 * Aggregate root representing a corporate bulk order and its lifecycle.
 *
 * <p>Invariants:
 * <ul>
 *   <li>At least one {@link OrderLine} must be present.</li>
 *   <li>A {@link CancellationReason} is required when transitioning to {@code CANCELLED}.</li>
 *   <li>Terminal-state orders cannot be transitioned further.</li>
 * </ul>
 *
 * <p>Layer: domain/model
 * <p>Module: order-service
 */
public final class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final UserId submittedBy;
    private final List<OrderLine> lines;
    private final OrderStatus status;
    private final CancellationReason cancellationReason;

    private Order(OrderId id, CustomerId customerId, UserId submittedBy,
                  List<OrderLine> lines, OrderStatus status,
                  CancellationReason cancellationReason) {
        this.id = Require.notNull(id, "Order.id");
        this.customerId = Require.notNull(customerId, "Order.customerId");
        this.submittedBy = Require.notNull(submittedBy, "Order.submittedBy");
        this.lines = List.copyOf(Require.notEmpty(lines, "Order.lines"));
        this.status = Require.notNull(status, "Order.status");
        this.cancellationReason = cancellationReason;
    }

    /** Creates a new order in {@code PENDING} status. */
    public static Order create(OrderId id, CustomerId customerId, UserId submittedBy,
                               List<OrderLine> lines) {
        return new Order(id, customerId, submittedBy, lines, OrderStatus.PENDING, null);
    }

    /** Transitions {@code PENDING} → {@code CONFIRMED}. */
    public Order confirm() {
        Require.isTrue(status == OrderStatus.PENDING,
                "confirm() requires PENDING status, current: " + status);
        return new Order(id, customerId, submittedBy, lines, OrderStatus.CONFIRMED, null);
    }

    /** Transitions {@code PENDING} → {@code AWAITING_CUSTOMER_DECISION}. */
    public Order awaitCustomerDecision() {
        Require.isTrue(status == OrderStatus.PENDING,
                "awaitCustomerDecision() requires PENDING status, current: " + status);
        return new Order(id, customerId, submittedBy, lines,
                OrderStatus.AWAITING_CUSTOMER_DECISION, null);
    }

    /** Transitions {@code AWAITING_CUSTOMER_DECISION} → {@code PARTIALLY_CONFIRMED}. */
    public Order confirmPartially() {
        Require.isTrue(status == OrderStatus.AWAITING_CUSTOMER_DECISION,
                "confirmPartially() requires AWAITING_CUSTOMER_DECISION status, current: " + status);
        return new Order(id, customerId, submittedBy, lines,
                OrderStatus.PARTIALLY_CONFIRMED, null);
    }

    /** Transitions any non-terminal state → {@code CANCELLED}. Requires a reason. */
    public Order cancel(CancellationReason reason) {
        Require.notNull(reason, "CancellationReason");
        Require.isTrue(!status.isTerminal(),
                "cancel() requires non-terminal status, current: " + status);
        return new Order(id, customerId, submittedBy, lines, OrderStatus.CANCELLED, reason);
    }

    /** Returns the unique order identifier. */
    public OrderId id() {
        return id;
    }

    /** Returns the identifier of the corporate customer who owns this order. */
    public CustomerId customerId() {
        return customerId;
    }

    /** Returns the identifier of the user who submitted this order. */
    public UserId submittedBy() {
        return submittedBy;
    }

    /** Returns an unmodifiable list of order lines. */
    public List<OrderLine> lines() {
        return lines;
    }

    /** Returns the current lifecycle status of this order. */
    public OrderStatus status() {
        return status;
    }

    /** Returns the cancellation reason, or {@code null} if the order is not cancelled. */
    public CancellationReason cancellationReason() {
        return cancellationReason;
    }
}
