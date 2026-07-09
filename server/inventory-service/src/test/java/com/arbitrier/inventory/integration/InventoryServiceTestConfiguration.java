package com.arbitrier.inventory.integration;

import com.arbitrier.inventory.adapter.outbound.ConfigurableStockAvailabilityPort;
import com.arbitrier.inventory.adapter.outbound.InMemoryStockReservationRepository;
import com.arbitrier.inventory.adapter.outbound.RecordingStockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.StockAvailabilityPort;
import com.arbitrier.inventory.application.port.outbound.StockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.StockReservationRepository;
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
    StockAvailabilityPort stockAvailabilityPort() {
        return new ConfigurableStockAvailabilityPort();
    }

    @Bean
    StockReservationEventPublisher stockReservationEventPublisher() {
        return new RecordingStockReservationEventPublisher();
    }
}
