package com.arbitrier.orchestrator.adapter.outbound;

import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Test adapter: HashMap-backed saga repository for unit and integration tests.
 *
 * <p>Layer: adapter/outbound (test)
 * <p>Module: orchestrator-service
 */
public class InMemorySagaRepository implements SagaRepository {

    private final Map<SagaId, Saga> store = new HashMap<>();

    @Override
    public void save(final Saga saga) {
        store.put(saga.id(), saga);
    }

    @Override
    public Optional<Saga> findById(final SagaId id) {
        return Optional.ofNullable(store.get(id));
    }

    /** Retrieves a saga by ID, throwing if absent. For assertions in tests. */
    public Saga getById(final SagaId id) {
        return findById(id).orElseThrow(
                () -> new AssertionError("Expected saga with id " + id + " but not found"));
    }

    /** Returns the number of sagas currently stored. */
    public int size() {
        return store.size();
    }
}
