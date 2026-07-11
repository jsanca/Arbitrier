package com.arbitrier.credit.adapter.outbound.persistence;

import com.arbitrier.credit.application.port.outbound.CreditReservationRepository;
import com.arbitrier.credit.domain.model.CreditReservation;
import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.platform.error.ApplicationProblemException;
import com.arbitrier.platform.error.PersistenceProblemCode;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Optional;

/**
 * JPA-backed implementation of {@link CreditReservationRepository}.
 *
 * <p>Translates infrastructure exceptions to typed {@link ApplicationProblemException}.
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
 * <p>Module: credit-service
 */
public class JpaCreditReservationRepositoryAdapter implements CreditReservationRepository {

    private final SpringDataCreditReservationRepository repository;
    private final CreditReservationPersistenceMapper mapper;

    public JpaCreditReservationRepositoryAdapter(
            SpringDataCreditReservationRepository repository,
            CreditReservationPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(CreditReservation reservation) {
        try {
            repository.save(mapper.toEntity(reservation));
        } catch (OptimisticLockingFailureException e) {
            throw new ApplicationProblemException(
                    PersistenceProblemCode.OPTIMISTIC_LOCK_CONFLICT,
                    "Concurrent modification of credit reservation " + reservation.id().value(), e);
        } catch (Exception e) {
            if (e instanceof ApplicationProblemException) {
                throw e;
            }
            throw new ApplicationProblemException(
                    PersistenceProblemCode.PERSISTENCE_FAILURE,
                    "Failed to save credit reservation " + reservation.id().value(), e);
        }
    }

    @Override
    public Optional<CreditReservation> findById(CreditReservationId id) {
        try {
            return repository.findById(id.value()).map(mapper::toDomain);
        } catch (Exception e) {
            throw new ApplicationProblemException(
                    PersistenceProblemCode.PERSISTENCE_FAILURE,
                    "Failed to load credit reservation " + id.value(), e);
        }
    }
}
