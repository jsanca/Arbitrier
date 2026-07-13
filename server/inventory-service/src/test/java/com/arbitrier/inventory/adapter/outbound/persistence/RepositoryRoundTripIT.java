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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository round-trip test for multi-warehouse stock reservations against a Flyway-migrated schema.
 *
 * <p>Complements FlywayMigrationIT and JpaStockReservationRepositoryAdapterTest. Verifies that
 * derived reservedQuantity() is correctly reconstructed from persisted allocations.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: inventory-service
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false"
        }
)
@Testcontainers
class RepositoryRoundTripIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("test-db/create-inventory-service-schema.sql");

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

    private static final StockReservationId RES_ID = StockReservationId.of("res-rt-1");
    private static final String ORDER_ID = "order-rt-1";

    private static final StockAllocation ALLOC_WH1 =
            new StockAllocation(WarehouseId.of("wh-rt-1"), "SKU-RT-A", 6);
    private static final StockAllocation ALLOC_WH2 =
            new StockAllocation(WarehouseId.of("wh-rt-2"), "SKU-RT-A", 4);
    private static final StockReservationLine FULL_LINE =
            new StockReservationLine("SKU-RT-A", 10, List.of(ALLOC_WH1, ALLOC_WH2));

    @BeforeEach
    void clean() {
        springDataStockReservationRepository.deleteById(RES_ID.value());
    }

    @Test
    void reservation_round_trip_with_multi_warehouse_allocations() {
        stockReservationRepository.save(
                StockReservation.fullyReserved(RES_ID, ORDER_ID, List.of(FULL_LINE)));

        StockReservation loaded = stockReservationRepository.findById(RES_ID).orElseThrow();

        assertThat(loaded.id()).isEqualTo(RES_ID);
        assertThat(loaded.orderId()).isEqualTo(ORDER_ID);
        assertThat(loaded.status()).isEqualTo(StockReservationStatus.RESERVED);
        assertThat(loaded.lines()).hasSize(1);

        StockReservationLine line = loaded.lines().get(0);
        assertThat(line.skuCode()).isEqualTo("SKU-RT-A");
        assertThat(line.requestedQuantity()).isEqualTo(10);
        assertThat(line.allocations()).hasSize(2);
        assertThat(line.reservedQuantity()).isEqualTo(10);
        assertThat(line.isFullyReserved()).isTrue();

        assertThat(line.allocations()).extracting(a -> a.warehouseId().value())
                .containsExactlyInAnyOrder("wh-rt-1", "wh-rt-2");
        assertThat(line.allocations()).extracting(StockAllocation::quantity)
                .containsExactlyInAnyOrder(6, 4);
        assertThat(loaded.version()).isNotNull();
    }
}
