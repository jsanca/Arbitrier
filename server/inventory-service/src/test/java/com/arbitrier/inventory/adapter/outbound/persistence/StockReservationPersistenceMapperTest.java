package com.arbitrier.inventory.adapter.outbound.persistence;

import com.arbitrier.inventory.domain.model.StockAllocation;
import com.arbitrier.inventory.domain.model.StockReservation;
import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.StockReservationLine;
import com.arbitrier.inventory.domain.model.StockReservationStatus;
import com.arbitrier.inventory.domain.model.WarehouseId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class StockReservationPersistenceMapperTest {

    private static final StockReservationPersistenceMapper MAPPER =
            new StockReservationPersistenceMapper();

    private static final StockReservationId RES_ID = StockReservationId.of("res-1");
    private static final String ORDER_ID = "order-1";

    private static final StockAllocation ALLOC_A =
            new StockAllocation(WarehouseId.of("wh-1"), "SKU-A", 10);
    private static final StockAllocation ALLOC_B =
            new StockAllocation(WarehouseId.of("wh-2"), "SKU-A", 5);
    private static final StockReservationLine FULL_LINE =
            new StockReservationLine("SKU-A", 10, List.of(ALLOC_A));
    private static final StockReservationLine PARTIAL_LINE =
            new StockReservationLine("SKU-A", 15, List.of(ALLOC_B));

    // ── toEntity / new reservation ────────────────────────────────────────────

    @Test
    void new_reservation_maps_to_entity_with_null_version() {
        StockReservation reservation = StockReservation.fullyReserved(RES_ID, ORDER_ID,
                List.of(FULL_LINE));

        StockReservationEntity entity = MAPPER.toEntity(reservation);

        assertThat(entity.getId()).isEqualTo("res-1");
        assertThat(entity.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(entity.getStatus()).isEqualTo("RESERVED");
        assertThat(entity.getVersion()).isNull();
        assertThat(entity.getLines()).hasSize(1);
    }

    @Test
    void toEntity_maps_line_with_parent_reference() {
        StockReservation reservation = StockReservation.fullyReserved(RES_ID, ORDER_ID,
                List.of(FULL_LINE));

        StockReservationEntity entity = MAPPER.toEntity(reservation);
        StockReservationLineEntity lineEntity = entity.getLines().get(0);

        assertThat(lineEntity.getReservation()).isSameAs(entity);
        assertThat(lineEntity.getSkuCode()).isEqualTo("SKU-A");
        assertThat(lineEntity.getRequestedQuantity()).isEqualTo(10);
    }

    @Test
    void toEntity_maps_allocation_with_parent_reference() {
        StockReservation reservation = StockReservation.fullyReserved(RES_ID, ORDER_ID,
                List.of(FULL_LINE));

        StockReservationEntity entity = MAPPER.toEntity(reservation);
        StockReservationLineEntity lineEntity = entity.getLines().get(0);
        StockAllocationEntity allocEntity = lineEntity.getAllocations().get(0);

        assertThat(allocEntity.getLine()).isSameAs(lineEntity);
        assertThat(allocEntity.getWarehouseId()).isEqualTo("wh-1");
        assertThat(allocEntity.getSku()).isEqualTo("SKU-A");
        assertThat(allocEntity.getQuantity()).isEqualTo(10);
    }

    // ── toDomain ─────────────────────────────────────────────────────────────

    @Test
    void entity_round_trips_to_domain_and_back() {
        StockReservation original = StockReservation.reconstruct(
                RES_ID, ORDER_ID, List.of(FULL_LINE), StockReservationStatus.RESERVED, 7L);

        StockReservationEntity entity = MAPPER.toEntity(original);
        entity.setVersion(7L);
        StockReservation restored = MAPPER.toDomain(entity);

        assertThat(restored.id()).isEqualTo(RES_ID);
        assertThat(restored.orderId()).isEqualTo(ORDER_ID);
        assertThat(restored.status()).isEqualTo(StockReservationStatus.RESERVED);
        assertThat(restored.version()).isEqualTo(7L);
        assertThat(restored.lines()).hasSize(1);
    }

    @Test
    void toDomain_restores_three_level_hierarchy() {
        StockAllocation allocWh1 = new StockAllocation(WarehouseId.of("wh-1"), "SKU-B", 3);
        StockAllocation allocWh2 = new StockAllocation(WarehouseId.of("wh-2"), "SKU-B", 2);
        StockReservationLine multiAllocLine =
                new StockReservationLine("SKU-B", 5, List.of(allocWh1, allocWh2));
        StockReservation original = StockReservation.reconstruct(
                RES_ID, ORDER_ID, List.of(multiAllocLine),
                StockReservationStatus.RESERVED, 3L);

        StockReservationEntity entity = MAPPER.toEntity(original);
        entity.setVersion(3L);
        StockReservation restored = MAPPER.toDomain(entity);

        StockReservationLine restoredLine = restored.lines().get(0);
        assertThat(restoredLine.skuCode()).isEqualTo("SKU-B");
        assertThat(restoredLine.requestedQuantity()).isEqualTo(5);
        assertThat(restoredLine.allocations()).hasSize(2);
        assertThat(restoredLine.allocations()).extracting(a -> a.warehouseId().value())
                .containsExactlyInAnyOrder("wh-1", "wh-2");
    }

    @Test
    void toDomain_restores_version() {
        StockReservation original = StockReservation.reconstruct(
                RES_ID, ORDER_ID, List.of(FULL_LINE), StockReservationStatus.RELEASED, 42L);

        StockReservationEntity entity = MAPPER.toEntity(original);
        entity.setVersion(42L);

        assertThat(MAPPER.toDomain(entity).version()).isEqualTo(42L);
    }

    // ── updateEntity ─────────────────────────────────────────────────────────

    @Test
    void updateEntity_replaces_lines_and_preserves_entity_version() {
        StockReservation initial = StockReservation.fullyReserved(RES_ID, ORDER_ID,
                List.of(FULL_LINE));
        StockReservationEntity entity = MAPPER.toEntity(initial);
        entity.setVersion(4L);

        StockAllocation newAlloc = new StockAllocation(WarehouseId.of("wh-3"), "SKU-C", 7);
        StockReservationLine newLine = new StockReservationLine("SKU-C", 7, List.of(newAlloc));
        StockReservation updated = StockReservation.reconstruct(
                RES_ID, ORDER_ID, List.of(newLine), StockReservationStatus.RELEASED, 4L);

        MAPPER.updateEntity(entity, updated);

        assertThat(entity.getVersion()).isEqualTo(4L);
        assertThat(entity.getStatus()).isEqualTo("RELEASED");
        assertThat(entity.getLines()).hasSize(1);
        assertThat(entity.getLines().get(0).getSkuCode()).isEqualTo("SKU-C");
    }

    // ── corrupt data ─────────────────────────────────────────────────────────

    @Test
    void toDomain_rejects_unknown_status() {
        StockReservationEntity entity = buildEntity("UNKNOWN_STATUS");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> MAPPER.toDomain(entity))
                .withMessageContaining("UNKNOWN_STATUS");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static StockReservationEntity buildEntity(String status) {
        StockReservationEntity entity = new StockReservationEntity();
        entity.setId("res-1");
        entity.setOrderId("order-1");
        entity.setStatus(status);
        entity.setVersion(1L);

        StockReservationLineEntity line = new StockReservationLineEntity();
        line.setReservation(entity);
        line.setSkuCode("SKU-A");
        line.setRequestedQuantity(5);

        StockAllocationEntity alloc = new StockAllocationEntity();
        alloc.setLine(line);
        alloc.setWarehouseId("wh-1");
        alloc.setSku("SKU-A");
        alloc.setQuantity(5);
        line.getAllocations().add(alloc);

        entity.getLines().add(line);
        return entity;
    }
}
