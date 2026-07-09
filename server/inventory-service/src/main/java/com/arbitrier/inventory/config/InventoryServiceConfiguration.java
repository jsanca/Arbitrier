package com.arbitrier.inventory.config;

import com.arbitrier.inventory.application.port.inbound.ReleaseStockUseCase;
import com.arbitrier.inventory.application.port.inbound.ReserveStockUseCase;
import com.arbitrier.inventory.application.port.outbound.StockAvailabilityPort;
import com.arbitrier.inventory.application.port.outbound.StockReservationEventPublisher;
import com.arbitrier.inventory.application.port.outbound.StockReservationRepository;
import com.arbitrier.inventory.application.service.ReleaseStockService;
import com.arbitrier.inventory.application.service.ReserveStockService;
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
            StockAvailabilityPort stockAvailabilityPort,
            StockReservationRepository repository,
            StockReservationEventPublisher eventPublisher) {
        return new ReserveStockService(stockAvailabilityPort, repository, eventPublisher);
    }

    @Bean
    ReleaseStockUseCase releaseStockUseCase(
            StockReservationRepository repository,
            StockReservationEventPublisher eventPublisher) {
        return new ReleaseStockService(repository, eventPublisher);
    }
}
