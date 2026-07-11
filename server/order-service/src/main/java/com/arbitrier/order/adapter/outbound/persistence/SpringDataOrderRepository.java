package com.arbitrier.order.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link OrderEntity}.
 *
 * <p>Used exclusively by {@link JpaOrderRepositoryAdapter}. Not accessible through
 * application ports.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: order-service
 */
public interface SpringDataOrderRepository extends JpaRepository<OrderEntity, String> {
}
