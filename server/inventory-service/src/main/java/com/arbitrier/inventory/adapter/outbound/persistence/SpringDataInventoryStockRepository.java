package com.arbitrier.inventory.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link InventoryStockEntity}.
 *
 * <p>Used exclusively by {@link JpaInventoryAvailabilityQueryAdapter}.
 * Batch retrieval via {@code findAllById} avoids N+1 queries.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: inventory-service
 */
public interface SpringDataInventoryStockRepository
        extends JpaRepository<InventoryStockEntity, String> {
}
