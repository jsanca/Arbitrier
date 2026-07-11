package com.arbitrier.orchestrator.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link SagaEntity}.
 *
 * <p>Used exclusively by {@link JpaSagaRepositoryAdapter}.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: orchestrator-service
 */
public interface SpringDataSagaRepository extends JpaRepository<SagaEntity, String> {
}
