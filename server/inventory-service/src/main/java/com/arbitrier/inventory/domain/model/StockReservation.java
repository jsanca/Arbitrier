package com.arbitrier.inventory.domain.model;

import com.arbitrier.platform.validation.Require;
import java.util.List;

/**
 * Aggregate root representing a stock reservation for a single order.
 *
 * <p>Warehouse allocation details are stored internally via the
 * {@link StockReservationLine} allocations. External callers see only
 * business-level outcomes (reserved quantity, status).
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@code RESERVED} requires all lines to be fully reserved.</li>
 *   <li>{@code PARTIALLY_RESERVED} requires at least one reserved line and at least one
 *       that is not fully reserved.</li>
 *   <li>{@link #release()} is idempotent: calling it on an already-released reservation
 *       returns {@code this}.</li>
 * </ul>
 *
 * <p>Layer: domain/model
 * <p>Module: inventory-service
 */
public final class StockReservation {

    private final StockReservationId id;
    private final String orderId;
    private final List<StockReservationLine> lines;
    private final StockReservationStatus status;
    /** Opaque optimistic-lock token set by the persistence adapter; null for new reservations. */
    private final Long version;

    private StockReservation(StockReservationId id, String orderId,
                              List<StockReservationLine> lines, StockReservationStatus status,
                              Long version) {
        this.id = Require.notNull(id, "StockReservation.id");
        this.orderId = Require.notBlank(orderId, "StockReservation.orderId");
        this.lines = List.copyOf(Require.notNull(lines, "StockReservation.lines"));
        this.status = Require.notNull(status, "StockReservation.status");
        this.version = version;
    }

    /**
     * Creates a fully-reserved reservation (all lines must be fully reserved).
     *
     * @throws IllegalArgumentException if any line is not fully reserved or the list is empty
     */
    public static StockReservation fullyReserved(StockReservationId id, String orderId,
                                                  List<StockReservationLine> lines) {
        Require.notEmpty(lines, "StockReservation.lines");
        Require.isTrue(lines.stream().allMatch(StockReservationLine::isFullyReserved),
                "fullyReserved() requires all lines to be fully reserved");
        return new StockReservation(id, orderId, lines, StockReservationStatus.RESERVED, null);
    }

    /**
     * Creates a partially-reserved reservation (at least one reserved, at least one not fully).
     *
     * @throws IllegalArgumentException if all lines are fully reserved, or none have reserved
     *                                  quantity, or the list is empty
     */
    public static StockReservation partiallyReserved(StockReservationId id, String orderId,
                                                      List<StockReservationLine> lines) {
        Require.notEmpty(lines, "StockReservation.lines");
        Require.isTrue(lines.stream().anyMatch(l -> l.reservedQuantity() > 0),
                "partiallyReserved() requires at least one line with reserved quantity");
        Require.isTrue(!lines.stream().allMatch(StockReservationLine::isFullyReserved),
                "partiallyReserved() cannot be used when all lines are fully reserved");
        return new StockReservation(id, orderId, lines, StockReservationStatus.PARTIALLY_RESERVED, null);
    }

    /**
     * Creates a rejected reservation (no stock reserved).
     *
     * @throws IllegalArgumentException if the list is empty
     */
    public static StockReservation rejected(StockReservationId id, String orderId,
                                             List<StockReservationLine> lines) {
        Require.notEmpty(lines, "StockReservation.lines");
        return new StockReservation(id, orderId, lines, StockReservationStatus.REJECTED, null);
    }

    /**
     * Rehydrates a reservation from a persistent store.
     *
     * <p>The {@code version} token must match what the adapter read from the database; it
     * is used to detect concurrent modifications when the reservation is later saved.
     */
    public static StockReservation reconstruct(StockReservationId id, String orderId,
                                               List<StockReservationLine> lines,
                                               StockReservationStatus status, Long version) {
        Require.notNull(id, "StockReservation.id");
        Require.notBlank(orderId, "StockReservation.orderId");
        Require.notNull(lines, "StockReservation.lines");
        Require.notNull(status, "StockReservation.status");
        return new StockReservation(id, orderId, lines, status, version);
    }

    /**
     * Releases reserved stock. Idempotent: if already {@code RELEASED}, returns {@code this}.
     */
    public StockReservation release() {
        if (status == StockReservationStatus.RELEASED) {
            return this;
        }
        return new StockReservation(id, orderId, lines, StockReservationStatus.RELEASED, version);
    }

    /** Returns the unique reservation identifier. */
    public StockReservationId id() {
        return id;
    }

    /** Returns the identifier of the order this reservation belongs to. */
    public String orderId() {
        return orderId;
    }

    /** Returns an unmodifiable list of reservation lines with internal allocation details. */
    public List<StockReservationLine> lines() {
        return lines;
    }

    /** Returns the current lifecycle status of this reservation. */
    public StockReservationStatus status() {
        return status;
    }

    /** Returns the opaque optimistic-lock token, or {@code null} for new (never-persisted) reservations. */
    public Long version() {
        return version;
    }
}
