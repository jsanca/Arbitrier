package com.arbitrier.orchestrator.adapter.outbound.persistence;

import com.arbitrier.orchestrator.domain.model.CustomerDecision;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import com.arbitrier.orchestrator.domain.model.SagaStep;
import com.arbitrier.platform.validation.Require;

/**
 * Maps between {@link Saga} domain aggregates and {@link SagaEntity} JPA entities.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: orchestrator-service
 */
public class SagaPersistenceMapper {

    /** Creates a new entity from the domain aggregate. */
    public SagaEntity toEntity(final Saga saga) {

        final SagaEntity entity = new SagaEntity();
        entity.setId(saga.id().value());
        entity.setOrderId(saga.orderId());
        entity.setCustomerId(saga.customerId());
        entity.setStatus(saga.status().name());
        entity.setCurrentStep(saga.currentStep().name());
        entity.setCustomerDecision(
                saga.customerDecision() != null ? saga.customerDecision().name() : null);
        entity.setStockReservationId(saga.stockReservationId());
        entity.setCreditReservationId(saga.creditReservationId());
        entity.setVersion(saga.version());
        return entity;
    }

    /** Updates an existing managed entity with state from the domain aggregate. */
    public SagaEntity updateEntity(final SagaEntity existing, final Saga saga) {

        existing.setOrderId(saga.orderId());
        existing.setCustomerId(saga.customerId());
        existing.setStatus(saga.status().name());
        existing.setCurrentStep(saga.currentStep().name());
        existing.setCustomerDecision(
                saga.customerDecision() != null ? saga.customerDecision().name() : null);
        existing.setStockReservationId(saga.stockReservationId());
        existing.setCreditReservationId(saga.creditReservationId());
        return existing;
    }

    /** Reconstructs a domain {@link Saga} from a {@link SagaEntity}. */
    public Saga toDomain(final SagaEntity entity) {

        Require.notNull(entity, "SagaEntity");

        final SagaStatus status = parseStatus(entity.getStatus());
        final SagaStep step = parseStep(entity.getCurrentStep());
        final CustomerDecision decision = parseCustomerDecision(entity.getCustomerDecision());

        return Saga.reconstruct(
                SagaId.of(entity.getId()),
                entity.getOrderId(),
                entity.getCustomerId(),
                status,
                step,
                decision,
                entity.getStockReservationId(),
                entity.getCreditReservationId(),
                entity.getVersion());
    }

    private static SagaStatus parseStatus(final String value) {
        try {
            return SagaStatus.valueOf(Require.notBlank(value, "SagaEntity.status"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unrecognised SagaStatus in persisted data: '" + value + "'", e);
        }
    }

    private static SagaStep parseStep(final String value) {
        try {
            return SagaStep.valueOf(Require.notBlank(value, "SagaEntity.currentStep"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unrecognised SagaStep in persisted data: '" + value + "'", e);
        }
    }

    private static CustomerDecision parseCustomerDecision(final String value) {
        if (value == null) {
            return null;
        }
        try {
            return CustomerDecision.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unrecognised CustomerDecision in persisted data: '" + value + "'", e);
        }
    }
}
