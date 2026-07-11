package com.arbitrier.inventory.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link StockReservationEntity}.
 *
 * <p>Used exclusively by {@link JpaStockReservationRepositoryAdapter}.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: inventory-service
 */
public interface SpringDataStockReservationRepository
        extends JpaRepository<StockReservationEntity, String> {
}
