package com.arbitrier.inventory.integration;

import com.arbitrier.inventory.adapter.outbound.ConfigurableWarehouseAllocationPort;
import com.arbitrier.inventory.adapter.outbound.InMemoryStockReservationRepository;
import com.arbitrier.inventory.application.port.outbound.StockReservationRepository;
import com.arbitrier.inventory.application.port.outbound.WarehouseAllocationPort;
import com.arbitrier.platform.messaging.inbox.InboxRepository;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.test.InMemoryInboxRepository;
import com.arbitrier.platform.messaging.test.InMemoryOutboxRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

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
    @Primary
    OutboxRepository outboxRepository() {
        return new InMemoryOutboxRepository();
    }

    @Bean
    @Primary
    InboxRepository inboxRepository() {
        return new InMemoryInboxRepository();
    }
}
