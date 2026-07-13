package com.arbitrier.inventory.adapter.outbound.persistence;

import com.arbitrier.inventory.adapter.outbound.ConfigurableWarehouseAllocationPort;
import com.arbitrier.inventory.adapter.outbound.RecordingStockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.StockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.StockReservationRepository;
import com.arbitrier.inventory.application.port.outbound.WarehouseAllocationPort;
import com.arbitrier.inventory.domain.model.StockAllocation;
import com.arbitrier.inventory.domain.model.StockReservation;
import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.StockReservationLine;
import com.arbitrier.inventory.domain.model.StockReservationStatus;
import com.arbitrier.inventory.domain.model.WarehouseId;
import com.arbitrier.platform.error.ApplicationProblemException;
import com.arbitrier.platform.error.PersistenceProblemCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the JPA stock-reservation persistence adapter using Testcontainers.
 *
 * <p>Verifies save/load with multi-warehouse allocations, status update, line replacement,
 * and optimistic-lock conflict detection.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.flyway.enabled=false"
        }
)
@Testcontainers
class JpaStockReservationRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("test-db/create-inventory-service-schema.sql");

    /** Stubs for non-persistence ports — StockReservationRepository is provided by JPA config. */
    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        WarehouseAllocationPort warehouseAllocationPort() {
            return new ConfigurableWarehouseAllocationPort();
        }

        @Bean
        @Primary
        StockReservationEventPublisher stockReservationEventPublisher() {
            return new RecordingStockReservationEventPublisher();
        }
    }

    @Autowired
    StockReservationRepository stockReservationRepository;

    @Autowired
    SpringDataStockReservationRepository springDataStockReservationRepository;

    private static final StockReservationId RES_ID = StockReservationId.of("res-tc-1");

    @BeforeEach
    void clean() {
        springDataStockReservationRepository.deleteById(RES_ID.value());
    }
    private static final String ORDER_ID = "order-1";

    private static final StockAllocation ALLOC_WH1 =
            new StockAllocation(WarehouseId.of("wh-1"), "SKU-A", 8);
    private static final StockAllocation ALLOC_WH2 =
            new StockAllocation(WarehouseId.of("wh-2"), "SKU-A", 2);
    private static final StockReservationLine FULL_LINE =
            new StockReservationLine("SKU-A", 10, List.of(ALLOC_WH1, ALLOC_WH2));

    // ── save and load ─────────────────────────────────────────────────────────

    @Test
    void save_and_load_new_reservation_with_multi_warehouse_allocations() {
        stockReservationRepository.save(
                StockReservation.fullyReserved(RES_ID, ORDER_ID, List.of(FULL_LINE)));

        Optional<StockReservation> loaded = stockReservationRepository.findById(RES_ID);

        assertThat(loaded).isPresent();
        StockReservation result = loaded.get();
        assertThat(result.id()).isEqualTo(RES_ID);
        assertThat(result.orderId()).isEqualTo(ORDER_ID);
        assertThat(result.status()).isEqualTo(StockReservationStatus.RESERVED);
        assertThat(result.lines()).hasSize(1);
        assertThat(result.lines().get(0).allocations()).hasSize(2);
        assertThat(result.version()).isNotNull();
    }

    @Test
    void save_preserves_all_allocation_details() {
        stockReservationRepository.save(
                StockReservation.fullyReserved(RES_ID, ORDER_ID, List.of(FULL_LINE)));

        StockReservation loaded = stockReservationRepository.findById(RES_ID).orElseThrow();
        List<StockAllocation> allocations = loaded.lines().get(0).allocations();

        assertThat(allocations).extracting(a -> a.warehouseId().value())
                .containsExactlyInAnyOrder("wh-1", "wh-2");
        assertThat(allocations).extracting(StockAllocation::quantity)
                .containsExactlyInAnyOrder(8, 2);
    }

    // ── status update ─────────────────────────────────────────────────────────

    @Test
    void save_updates_reservation_status_to_released() {
        stockReservationRepository.save(
                StockReservation.fullyReserved(RES_ID, ORDER_ID, List.of(FULL_LINE)));

        StockReservation loaded = stockReservationRepository.findById(RES_ID).orElseThrow();
        stockReservationRepository.save(loaded.release());

        StockReservation updated = stockReservationRepository.findById(RES_ID).orElseThrow();
        assertThat(updated.status()).isEqualTo(StockReservationStatus.RELEASED);
    }

    // ── optimistic lock conflict ──────────────────────────────────────────────

    @Test
    void optimistic_lock_conflict_throws_typed_exception() {
        stockReservationRepository.save(
                StockReservation.fullyReserved(RES_ID, ORDER_ID, List.of(FULL_LINE)));

        StockReservation staleLoad = stockReservationRepository.findById(RES_ID).orElseThrow();
        // Advance DB version by saving once with the current version
        stockReservationRepository.save(staleLoad.release());

        // staleLoad still carries version 0; DB is now at version 1
        assertThatThrownBy(() -> stockReservationRepository.save(staleLoad.release()))
                .isInstanceOf(ApplicationProblemException.class)
                .satisfies(ex -> assertThat(((ApplicationProblemException) ex).code())
                        .isEqualTo(PersistenceProblemCode.OPTIMISTIC_LOCK_CONFLICT));
    }

    // ── findById not found ────────────────────────────────────────────────────

    @Test
    void findById_returns_empty_for_unknown_id() {
        Optional<StockReservation> result =
                stockReservationRepository.findById(StockReservationId.of("not-existing"));

        assertThat(result).isEmpty();
    }

    // ── line replacement ──────────────────────────────────────────────────────

    @Test
    void save_replaces_lines_atomically_on_update() {
        stockReservationRepository.save(
                StockReservation.fullyReserved(RES_ID, ORDER_ID, List.of(FULL_LINE)));

        StockReservation loaded = stockReservationRepository.findById(RES_ID).orElseThrow();
        StockAllocation newAlloc = new StockAllocation(WarehouseId.of("wh-3"), "SKU-B", 5);
        StockReservationLine newLine = new StockReservationLine("SKU-B", 5, List.of(newAlloc));
        StockReservation updated = StockReservation.reconstruct(
                loaded.id(), loaded.orderId(), List.of(newLine),
                StockReservationStatus.PARTIALLY_RESERVED, loaded.version());
        stockReservationRepository.save(updated);

        StockReservation reloaded = stockReservationRepository.findById(RES_ID).orElseThrow();
        assertThat(reloaded.lines()).hasSize(1);
        assertThat(reloaded.lines().get(0).skuCode()).isEqualTo("SKU-B");
        assertThat(reloaded.lines().get(0).allocations().get(0).warehouseId().value())
                .isEqualTo("wh-3");
    }
}
