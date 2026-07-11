package com.arbitrier.orchestrator.adapter.outbound.persistence;

import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.platform.error.ApplicationProblemException;
import com.arbitrier.platform.error.PersistenceProblemCode;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Optional;

/**
 * JPA-backed implementation of {@link SagaRepository}.
 *
 * <p>The saga is the source of truth for saga state. All transitions are persisted via
 * explicit column updates — no serialized workflow blobs.
 *
 * <p>Optimistic locking: Hibernate's native {@code @Version} check runs on flush;
 * if a concurrent transaction has already modified the row, Spring Data throws
 * {@link OptimisticLockingFailureException}, which is mapped to
 * {@link PersistenceProblemCode#OPTIMISTIC_LOCK_CONFLICT}.
 *
 * <p>Transactionality: this adapter is not {@code @Transactional}. The owning application
 * service provides the transaction boundary via {@code @Transactional}; adapter calls
 * participate through the default {@code REQUIRED} propagation.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: orchestrator-service
 */
public class JpaSagaRepositoryAdapter implements SagaRepository {

    private final SpringDataSagaRepository repository;
    private final SagaPersistenceMapper mapper;

    public JpaSagaRepositoryAdapter(SpringDataSagaRepository repository,
                                    SagaPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(Saga saga) {
        try {
            repository.save(mapper.toEntity(saga));
        } catch (OptimisticLockingFailureException e) {
            throw new ApplicationProblemException(
                    PersistenceProblemCode.OPTIMISTIC_LOCK_CONFLICT,
                    "Concurrent modification of saga " + saga.id().value(), e);
        } catch (Exception e) {
            if (e instanceof ApplicationProblemException) {
                throw e;
            }
            throw new ApplicationProblemException(
                    PersistenceProblemCode.PERSISTENCE_FAILURE,
                    "Failed to save saga " + saga.id().value(), e);
        }
    }

    @Override
    public Optional<Saga> findById(SagaId id) {
        try {
            return repository.findById(id.value()).map(mapper::toDomain);
        } catch (Exception e) {
            throw new ApplicationProblemException(
                    PersistenceProblemCode.PERSISTENCE_FAILURE,
                    "Failed to load saga " + id.value(), e);
        }
    }
}
