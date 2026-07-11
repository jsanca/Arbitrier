package com.arbitrier.inventory.adapter.outbound.persistence;

import com.arbitrier.inventory.application.port.outbound.StockReservationRepository;
import com.arbitrier.inventory.domain.model.StockReservation;
import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.platform.error.ApplicationProblemException;
import com.arbitrier.platform.error.PersistenceProblemCode;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Optional;

/**
 * JPA-backed implementation of {@link StockReservationRepository}.
 *
 * <p>Translates infrastructure exceptions to typed {@link ApplicationProblemException}.
 * The full allocation graph (reservation → lines → allocations) is loaded eagerly so
 * that no lazy proxy escapes the adapter boundary.
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
 * <p>Module: inventory-service
 */
public class JpaStockReservationRepositoryAdapter implements StockReservationRepository {

    private final SpringDataStockReservationRepository repository;
    private final StockReservationPersistenceMapper mapper;

    public JpaStockReservationRepositoryAdapter(SpringDataStockReservationRepository repository,
                                                StockReservationPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(StockReservation reservation) {
        try {
            repository.save(mapper.toEntity(reservation));
        } catch (OptimisticLockingFailureException e) {
            throw new ApplicationProblemException(
                    PersistenceProblemCode.OPTIMISTIC_LOCK_CONFLICT,
                    "Concurrent modification of stock reservation " + reservation.id().value(), e);
        } catch (Exception e) {
            if (e instanceof ApplicationProblemException) {
                throw e;
            }
            throw new ApplicationProblemException(
                    PersistenceProblemCode.PERSISTENCE_FAILURE,
                    "Failed to save stock reservation " + reservation.id().value(), e);
        }
    }

    @Override
    public Optional<StockReservation> findById(StockReservationId id) {
        try {
            return repository.findById(id.value()).map(mapper::toDomain);
        } catch (Exception e) {
            throw new ApplicationProblemException(
                    PersistenceProblemCode.PERSISTENCE_FAILURE,
                    "Failed to load stock reservation " + id.value(), e);
        }
    }
}
