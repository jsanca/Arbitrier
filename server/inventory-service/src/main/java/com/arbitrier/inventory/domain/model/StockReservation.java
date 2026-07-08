package com.arbitrier.inventory.domain.model;

import com.arbitrier.platform.validation.Require;
import java.util.List;

/**
 * Aggregate root representing a stock reservation for a single order.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@code RESERVED} requires all lines to be fully reserved.</li>
 *   <li>{@code PARTIALLY_RESERVED} requires at least one reserved line and at least one
 *       unreserved line.</li>
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
    private final WarehouseId warehouseId;
    private final List<StockReservationLine> lines;
    private final StockReservationStatus status;

    private StockReservation(StockReservationId id, String orderId, WarehouseId warehouseId,
                              List<StockReservationLine> lines, StockReservationStatus status) {
        this.id = Require.notNull(id, "StockReservation.id");
        this.orderId = Require.notBlank(orderId, "StockReservation.orderId");
        this.warehouseId = Require.notNull(warehouseId, "StockReservation.warehouseId");
        this.lines = List.copyOf(Require.notNull(lines, "StockReservation.lines"));
        this.status = Require.notNull(status, "StockReservation.status");
    }

    /**
     * Creates a fully-reserved reservation (all lines must have
     * {@code reservedQuantity == requestedQuantity}).
     *
     * @throws IllegalArgumentException if any line is not fully reserved or the list is empty
     */
    public static StockReservation fullyReserved(StockReservationId id, String orderId,
                                                  WarehouseId warehouseId,
                                                  List<StockReservationLine> lines) {
        Require.notEmpty(lines, "StockReservation.lines");
        Require.isTrue(lines.stream().allMatch(StockReservationLine::isFullyReserved),
                "fullyReserved() requires all lines to be fully reserved");
        return new StockReservation(id, orderId, warehouseId, lines,
                StockReservationStatus.RESERVED);
    }

    /**
     * Creates a partially-reserved reservation (at least one reserved, at least one unreserved).
     *
     * @throws IllegalArgumentException if all lines are fully reserved, or none have any reserved
     *                                  quantity, or the list is empty
     */
    public static StockReservation partiallyReserved(StockReservationId id, String orderId,
                                                      WarehouseId warehouseId,
                                                      List<StockReservationLine> lines) {
        Require.notEmpty(lines, "StockReservation.lines");
        Require.isTrue(lines.stream().anyMatch(l -> l.reservedQuantity() > 0),
                "partiallyReserved() requires at least one line with reserved quantity");
        Require.isTrue(!lines.stream().allMatch(StockReservationLine::isFullyReserved),
                "partiallyReserved() cannot be used when all lines are fully reserved");
        return new StockReservation(id, orderId, warehouseId, lines,
                StockReservationStatus.PARTIALLY_RESERVED);
    }

    /**
     * Creates a rejected reservation (no stock reserved).
     *
     * @throws IllegalArgumentException if the list is empty
     */
    public static StockReservation rejected(StockReservationId id, String orderId,
                                             WarehouseId warehouseId,
                                             List<StockReservationLine> lines) {
        Require.notEmpty(lines, "StockReservation.lines");
        return new StockReservation(id, orderId, warehouseId, lines,
                StockReservationStatus.REJECTED);
    }

    /**
     * Releases reserved stock. Idempotent: if already {@code RELEASED}, returns {@code this}.
     */
    public StockReservation release() {
        if (status == StockReservationStatus.RELEASED) {
            return this;
        }
        return new StockReservation(id, orderId, warehouseId, lines,
                StockReservationStatus.RELEASED);
    }

    /** Returns the unique reservation identifier. */
    public StockReservationId id() {
        return id;
    }

    /** Returns the identifier of the order this reservation belongs to. */
    public String orderId() {
        return orderId;
    }

    /** Returns the warehouse from which stock is reserved. */
    public WarehouseId warehouseId() {
        return warehouseId;
    }

    /** Returns an unmodifiable list of reservation lines. */
    public List<StockReservationLine> lines() {
        return lines;
    }

    /** Returns the current lifecycle status of this reservation. */
    public StockReservationStatus status() {
        return status;
    }
}
