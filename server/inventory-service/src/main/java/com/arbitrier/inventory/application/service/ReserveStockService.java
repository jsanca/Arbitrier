package com.arbitrier.inventory.application.service;

import com.arbitrier.inventory.application.port.inbound.ReserveStockCommand;
import com.arbitrier.inventory.application.port.inbound.ReserveStockResult;
import com.arbitrier.inventory.application.port.inbound.ReserveStockUseCase;
import com.arbitrier.inventory.application.port.outbound.StockAvailabilityPort;
import com.arbitrier.inventory.application.port.outbound.StockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.StockReservationRepository;
import com.arbitrier.inventory.domain.event.StockPartiallyReservedDomainEvent;
import com.arbitrier.inventory.domain.event.StockRejectedDomainEvent;
import com.arbitrier.inventory.domain.event.StockReservedDomainEvent;
import com.arbitrier.inventory.domain.model.StockReservation;
import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.StockReservationLine;
import com.arbitrier.inventory.domain.model.StockReservationStatus;
import com.arbitrier.inventory.domain.model.WarehouseId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Use-case implementation: reserve stock for an order.
 *
 * <h2>Reservation outcomes</h2>
 * <ul>
 *   <li><b>RESERVED</b> — all requested quantities are available; all lines fully reserved.</li>
 *   <li><b>PARTIALLY_RESERVED</b> — at least one line can be reserved but not all lines are
 *       fully available.</li>
 *   <li><b>REJECTED</b> — no requested line has any available stock.</li>
 * </ul>
 *
 * <h2>Per-line allocation logic</h2>
 * <p>For each line, {@link StockAvailabilityPort#availableQuantity(WarehouseId, String)} is
 * queried. The reserved quantity is {@code min(requested, available)}. There is no warehouse
 * optimisation or split-across-warehouse logic in this slice.
 *
 * <h2>Transactionality (deferred)</h2>
 * <p>This service will become {@code @Transactional} when JPA persistence is introduced.
 * DB + Kafka consistency will be handled by the Outbox pattern — the event will be written
 * to an outbox table inside the same DB transaction rather than being published directly here.
 *
 * <p>Layer: application/service
 * <p>Module: inventory-service
 */
public class ReserveStockService implements ReserveStockUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReserveStockService.class);

    private final StockAvailabilityPort stockAvailabilityPort;
    private final StockReservationRepository repository;
    private final StockReservationEventPublisher eventPublisher;

    public ReserveStockService(
            StockAvailabilityPort stockAvailabilityPort,
            StockReservationRepository repository,
            StockReservationEventPublisher eventPublisher) {
        this.stockAvailabilityPort = stockAvailabilityPort;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ReserveStockResult reserve(final ReserveStockCommand command) {

        final StockReservationId reservationId = StockReservationId.of(command.reservationId());
        final WarehouseId warehouseId = WarehouseId.of(command.warehouseId());

        final List<StockReservationLine> lines = allocate(command, warehouseId);
        final StockReservation reservation = createReservation(reservationId, command.orderId(), warehouseId, lines);

        repository.save(reservation);
        publishEvent(reservation, command.orderId(), warehouseId, lines);

        log.info("Stock reservation complete reservationId={} orderId={} warehouseId={} outcome={}",
                reservationId, command.orderId(), warehouseId, reservation.status());

        return new ReserveStockResult(reservationId, reservation.status());
    }

    private List<StockReservationLine> allocate(final ReserveStockCommand command, final WarehouseId warehouseId) {
        return command.lines().stream()
                .map(line -> {
                    final int available = stockAvailabilityPort.availableQuantity(warehouseId, line.sku());
                    final int reserved = Math.min(line.quantity(), available);
                    return new StockReservationLine(line.sku(), line.quantity(), reserved);
                })
                .toList();
    }

    private StockReservation createReservation(StockReservationId id, String orderId,
                                               WarehouseId warehouseId,
                                               List<StockReservationLine> lines) {

        final boolean allFullyReserved = lines.stream().allMatch(StockReservationLine::isFullyReserved);
        final boolean anyReserved = lines.stream().anyMatch(l -> l.reservedQuantity() > 0);

        if (allFullyReserved) {

            return StockReservation.fullyReserved(id, orderId, warehouseId, lines);
        } else if (anyReserved) {

            return StockReservation.partiallyReserved(id, orderId, warehouseId, lines);
        }

        return StockReservation.rejected(id, orderId, warehouseId, lines);
    }

    private void publishEvent(StockReservation reservation, String orderId,
                               WarehouseId warehouseId, List<StockReservationLine> lines) {
        final StockReservationId id = reservation.id();
        switch (reservation.status()) {
            case RESERVED -> eventPublisher.publishReserved(
                    new StockReservedDomainEvent(id, orderId, warehouseId, lines));
            case PARTIALLY_RESERVED -> eventPublisher.publishPartiallyReserved(
                    new StockPartiallyReservedDomainEvent(id, orderId, warehouseId, lines));
            case REJECTED -> eventPublisher.publishRejected(
                    new StockRejectedDomainEvent(id, orderId, warehouseId));
            default -> throw new IllegalStateException("Unexpected reservation status: " + reservation.status());
        }
    }
}
