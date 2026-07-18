package com.arbitrier.inventory.config;

import com.arbitrier.inventory.adapter.inbound.grpc.InventoryAvailabilityGrpcRequestMapper;
import com.arbitrier.inventory.adapter.inbound.grpc.InventoryAvailabilityGrpcResponseMapper;
import com.arbitrier.inventory.adapter.inbound.grpc.InventoryGrpcExceptionMapper;
import com.arbitrier.inventory.application.port.inbound.CheckInventoryAvailabilityUseCase;
import com.arbitrier.inventory.application.port.inbound.CheckStockAvailabilityUseCase;
import com.arbitrier.inventory.application.port.inbound.ReleaseStockUseCase;
import com.arbitrier.inventory.application.port.inbound.ReserveStockUseCase;
import com.arbitrier.inventory.application.port.outbound.InventoryAvailabilityQueryPort;
import com.arbitrier.inventory.application.port.outbound.StockReservationRepository;
import com.arbitrier.inventory.application.port.outbound.WarehouseAllocationPort;
import com.arbitrier.inventory.application.service.CheckInventoryAvailabilityService;
import com.arbitrier.inventory.application.service.CheckStockAvailabilityService;
import com.arbitrier.inventory.application.service.ReleaseStockService;
import com.arbitrier.inventory.application.service.ReserveStockService;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring for the inventory-service application layer.
 *
 * <p>Layer: config
 * <p>Module: inventory-service
 */
@Configuration
public class InventoryServiceConfiguration {

    @Bean
    ReserveStockUseCase reserveStockUseCase(
            final WarehouseAllocationPort warehouseAllocationPort,
            final StockReservationRepository repository,
            final OutboxRepository outboxRepository,
            final DomainEventToOutboxMapper outboxMapper) {
        return new ReserveStockService(warehouseAllocationPort, repository, outboxRepository, outboxMapper);
    }

    @Bean
    ReleaseStockUseCase releaseStockUseCase(
            final StockReservationRepository repository,
            final OutboxRepository outboxRepository,
            final DomainEventToOutboxMapper outboxMapper) {
        return new ReleaseStockService(repository, outboxRepository, outboxMapper);
    }

    @Bean
    CheckStockAvailabilityUseCase checkStockAvailabilityUseCase(
            final WarehouseAllocationPort warehouseAllocationPort) {
        return new CheckStockAvailabilityService(warehouseAllocationPort);
    }

    @Bean
    CheckInventoryAvailabilityUseCase checkInventoryAvailabilityUseCase(
            final InventoryAvailabilityQueryPort inventoryAvailabilityQueryPort) {
        return new CheckInventoryAvailabilityService(inventoryAvailabilityQueryPort);
    }

    @Bean
    InventoryAvailabilityGrpcRequestMapper inventoryAvailabilityGrpcRequestMapper() {
        return new InventoryAvailabilityGrpcRequestMapper();
    }

    @Bean
    InventoryAvailabilityGrpcResponseMapper inventoryAvailabilityGrpcResponseMapper() {
        return new InventoryAvailabilityGrpcResponseMapper();
    }

    @Bean
    InventoryGrpcExceptionMapper inventoryGrpcExceptionMapper() {
        return new InventoryGrpcExceptionMapper();
    }
}
