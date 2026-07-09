package com.arbitrier.orchestrator.application.port.outbound;

import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;

import java.util.Optional;

/**
 * Outbound port: persists and loads {@link Saga} aggregates.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: orchestrator-service
 */
public interface SagaRepository {

    /** Persists or updates the given saga instance. */
    void save(Saga saga);

    /** Loads a saga by its identifier, returning empty if not found. */
    Optional<Saga> findById(SagaId id);
}
