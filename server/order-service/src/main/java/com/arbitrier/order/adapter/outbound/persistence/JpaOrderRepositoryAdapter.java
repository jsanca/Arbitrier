package com.arbitrier.order.adapter.outbound.persistence;

import com.arbitrier.order.application.port.outbound.OrderRepository;
import com.arbitrier.order.domain.model.Order;
import com.arbitrier.order.domain.model.OrderId;
import com.arbitrier.platform.error.ApplicationProblemException;
import com.arbitrier.platform.error.PersistenceProblemCode;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Optional;

/**
 * JPA-backed implementation of {@link OrderRepository}.
 *
 * <p>Translates infrastructure exceptions to typed {@link ApplicationProblemException} so
 * that JPA and Spring Data exceptions never cross the adapter boundary.
 *
 * <p>Optimistic locking: the domain {@link Order} carries a {@code version} token. On save,
 * {@link org.springframework.data.jpa.repository.JpaRepository#save} issues
 * {@code em.merge()}, which causes Hibernate to generate
 * {@code UPDATE ... WHERE id = ? AND version = <domain_version>}.
 * If a concurrent transaction has already modified the row, Hibernate throws
 * {@link OptimisticLockingFailureException}, which is mapped to
 * {@link PersistenceProblemCode#OPTIMISTIC_LOCK_CONFLICT}.
 *
 * <p>Transactionality: this adapter is not {@code @Transactional}. The owning application
 * service provides the transaction boundary via {@code @Transactional}; adapter calls
 * participate through the default {@code REQUIRED} propagation.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: order-service
 */
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final SpringDataOrderRepository repository;
    private final OrderPersistenceMapper mapper;

    public JpaOrderRepositoryAdapter(SpringDataOrderRepository repository,
                                     OrderPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(Order order) {
        try {
            repository.save(mapper.toEntity(order));
        } catch (OptimisticLockingFailureException e) {
            throw new ApplicationProblemException(
                    PersistenceProblemCode.OPTIMISTIC_LOCK_CONFLICT,
                    "Concurrent modification of order " + order.id().value(), e);
        } catch (Exception e) {
            if (e instanceof ApplicationProblemException) {
                throw e;
            }
            throw new ApplicationProblemException(
                    PersistenceProblemCode.PERSISTENCE_FAILURE,
                    "Failed to save order " + order.id().value(), e);
        }
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        try {
            return repository.findById(id.value()).map(mapper::toDomain);
        } catch (Exception e) {
            throw new ApplicationProblemException(
                    PersistenceProblemCode.PERSISTENCE_FAILURE,
                    "Failed to load order " + id.value(), e);
        }
    }
}
