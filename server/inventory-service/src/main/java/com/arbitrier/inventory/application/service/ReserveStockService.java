package com.arbitrier.inventory.application.service;

import com.arbitrier.inventory.application.port.inbound.ReserveStockCommand;
import com.arbitrier.inventory.application.port.inbound.ReserveStockResult;
import com.arbitrier.inventory.application.port.inbound.ReserveStockUseCase;
import com.arbitrier.inventory.application.port.outbound.AllocationPlan;
import com.arbitrier.inventory.application.port.outbound.RequestedStockLine;
import com.arbitrier.inventory.application.port.outbound.StockReservationRepository;
import com.arbitrier.inventory.application.port.outbound.WarehouseAllocationPort;
import com.arbitrier.inventory.domain.event.StockPartiallyReservedDomainEvent;
import com.arbitrier.inventory.domain.event.StockRejectedDomainEvent;
import com.arbitrier.inventory.domain.event.StockReservedDomainEvent;
import com.arbitrier.inventory.domain.model.StockReservation;
import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.StockReservationLine;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

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
 * <h2>Multi-warehouse allocation</h2>
 * <p>{@link WarehouseAllocationPort#allocate(List)} returns an {@link AllocationPlan} that
 * may span multiple warehouses. Internal warehouse identifiers are stored in the reservation
 * lines but are never exposed outside the Inventory bounded context.
 *
 * <p>Layer: application/service
 * <p>Module: inventory-service
 */
public class ReserveStockService implements ReserveStockUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReserveStockService.class);
    private static final String AGGREGATE_TYPE = "StockReservation";

    private final WarehouseAllocationPort warehouseAllocationPort;
    private final StockReservationRepository repository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;

    public ReserveStockService(
            final WarehouseAllocationPort warehouseAllocationPort,
            final StockReservationRepository repository,
            final OutboxRepository outboxRepository,
            final DomainEventToOutboxMapper outboxMapper) {
        this.warehouseAllocationPort = Require.notNull(warehouseAllocationPort, "warehouseAllocationPort");
        this.repository = Require.notNull(repository, "repository");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.outboxMapper = Require.notNull(outboxMapper, "outboxMapper");
    }

    @Override
    @Transactional
    public ReserveStockResult reserve(final ReserveStockCommand command) {

        final StockReservationId reservationId = StockReservationId.of(command.reservationId());
        final List<StockReservationLine> lines = allocate(command);
        final StockReservation reservation = createReservation(reservationId, command.orderId(), lines);

        repository.save(reservation);
        publishEvent(reservation, command.orderId(), lines);

        log.info("Stock reservation complete reservationId={} orderId={} outcome={}",
                reservationId, command.orderId(), reservation.status());

        return new ReserveStockResult(reservationId, reservation.status());
    }

    private List<StockReservationLine> allocate(final ReserveStockCommand command) {
        final List<RequestedStockLine> requestedLines = command.lines().stream()
                .map(l -> new RequestedStockLine(l.sku(), l.quantity()))
                .toList();
        final AllocationPlan plan = warehouseAllocationPort.allocate(requestedLines);
        return command.lines().stream()
                .map(line -> new StockReservationLine(line.sku(), line.quantity(), plan.forSku(line.sku())))
                .toList();
    }

    private StockReservation createReservation(final StockReservationId id, final String orderId,
                                               final List<StockReservationLine> lines) {
        final boolean allFullyReserved = lines.stream().allMatch(StockReservationLine::isFullyReserved);
        final boolean anyReserved = lines.stream().anyMatch(l -> l.reservedQuantity() > 0);

        if (allFullyReserved) {
            return StockReservation.fullyReserved(id, orderId, lines);
        } else if (anyReserved) {
            return StockReservation.partiallyReserved(id, orderId, lines);
        }
        return StockReservation.rejected(id, orderId, lines);
    }

    private void publishEvent(final StockReservation reservation, final String orderId,
                               final List<StockReservationLine> lines) {
        final StockReservationId id = reservation.id();
        final Object event = switch (reservation.status()) {
            case RESERVED -> new StockReservedDomainEvent(id, orderId, lines);
            case PARTIALLY_RESERVED -> new StockPartiallyReservedDomainEvent(id, orderId, lines);
            case REJECTED -> new StockRejectedDomainEvent(id, orderId);
            default -> throw new IllegalStateException("Unexpected reservation status: " + reservation.status());
        };
        outboxRepository.save(outboxMapper.map(event, id.value(), AGGREGATE_TYPE));
    }
}
