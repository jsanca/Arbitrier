package com.arbitrier.inventory.integration;

import com.arbitrier.inventory.adapter.outbound.ConfigurableWarehouseAllocationPort;
import com.arbitrier.inventory.adapter.outbound.InMemoryStockReservationRepository;
import com.arbitrier.inventory.adapter.outbound.RecordingStockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.StockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.StockReservationRepository;
import com.arbitrier.inventory.application.port.outbound.WarehouseAllocationPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test-only beans for the inventory-service Spring context.
 *
 * <p>Provides in-memory adapters for all outbound ports — no Postgres, Kafka, or
 * Docker required.
 */
@TestConfiguration
public class InventoryServiceTestConfiguration {

    @Bean
    StockReservationRepository stockReservationRepository() {
        return new InMemoryStockReservationRepository();
    }

    @Bean
    WarehouseAllocationPort warehouseAllocationPort() {
        return new ConfigurableWarehouseAllocationPort();
    }

    @Bean
    StockReservationEventPublisher stockReservationEventPublisher() {
        return new RecordingStockReservationEventPublisher();
    }
}
