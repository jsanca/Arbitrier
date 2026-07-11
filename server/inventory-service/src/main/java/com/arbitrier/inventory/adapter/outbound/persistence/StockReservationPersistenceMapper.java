package com.arbitrier.inventory.adapter.outbound.persistence;

import com.arbitrier.inventory.domain.model.StockAllocation;
import com.arbitrier.inventory.domain.model.StockReservation;
import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.StockReservationLine;
import com.arbitrier.inventory.domain.model.StockReservationStatus;
import com.arbitrier.inventory.domain.model.WarehouseId;
import com.arbitrier.platform.validation.Require;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps between {@link StockReservation} domain aggregates and their JPA entity graph.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: inventory-service
 */
public class StockReservationPersistenceMapper {

    /** Creates a new entity graph from the domain aggregate. */
    public StockReservationEntity toEntity(StockReservation reservation) {
        StockReservationEntity entity = new StockReservationEntity();
        entity.setId(reservation.id().value());
        entity.setOrderId(reservation.orderId());
        entity.setStatus(reservation.status().name());
        entity.setVersion(reservation.version());

        List<StockReservationLineEntity> lineEntities = reservation.lines().stream()
                .map(line -> toLineEntity(line, entity))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        entity.setLines(lineEntities);

        return entity;
    }

    /**
     * Updates an existing managed entity with state from the domain aggregate.
     * Clears and replaces lines and allocations atomically.
     */
    public StockReservationEntity updateEntity(StockReservationEntity existing,
                                               StockReservation reservation) {
        existing.setOrderId(reservation.orderId());
        existing.setStatus(reservation.status().name());

        existing.getLines().clear();
        reservation.lines().forEach(line -> {
            StockReservationLineEntity lineEntity = toLineEntity(line, existing);
            existing.getLines().add(lineEntity);
        });

        return existing;
    }

    /** Reconstructs a domain {@link StockReservation} from its entity graph. */
    public StockReservation toDomain(StockReservationEntity entity) {
        Require.notNull(entity, "StockReservationEntity");

        StockReservationStatus status = parseStatus(entity.getStatus());
        List<StockReservationLine> lines = entity.getLines().stream()
                .map(this::toLine)
                .toList();

        return StockReservation.reconstruct(
                StockReservationId.of(entity.getId()),
                entity.getOrderId(),
                lines,
                status,
                entity.getVersion());
    }

    private StockReservationLineEntity toLineEntity(StockReservationLine line,
                                                     StockReservationEntity parent) {
        StockReservationLineEntity lineEntity = new StockReservationLineEntity();
        lineEntity.setReservation(parent);
        lineEntity.setSkuCode(line.skuCode());
        lineEntity.setRequestedQuantity(line.requestedQuantity());

        line.allocations().forEach(allocation -> {
            StockAllocationEntity alloc = toAllocationEntity(allocation, lineEntity);
            lineEntity.getAllocations().add(alloc);
        });

        return lineEntity;
    }

    private StockAllocationEntity toAllocationEntity(StockAllocation allocation,
                                                      StockReservationLineEntity parent) {
        StockAllocationEntity entity = new StockAllocationEntity();
        entity.setLine(parent);
        entity.setWarehouseId(allocation.warehouseId().value());
        entity.setSku(allocation.sku());
        entity.setQuantity(allocation.quantity());
        return entity;
    }

    private StockReservationLine toLine(StockReservationLineEntity entity) {
        List<StockAllocation> allocations = entity.getAllocations().stream()
                .map(this::toAllocation)
                .toList();
        return new StockReservationLine(entity.getSkuCode(), entity.getRequestedQuantity(),
                allocations);
    }

    private StockAllocation toAllocation(StockAllocationEntity entity) {
        return new StockAllocation(WarehouseId.of(entity.getWarehouseId()),
                entity.getSku(), entity.getQuantity());
    }

    private static StockReservationStatus parseStatus(String value) {
        try {
            return StockReservationStatus.valueOf(
                    Require.notBlank(value, "StockReservationEntity.status"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unrecognised StockReservationStatus in persisted data: '" + value + "'", e);
        }
    }
}
