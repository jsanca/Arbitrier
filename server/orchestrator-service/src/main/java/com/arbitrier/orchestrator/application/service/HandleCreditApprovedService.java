package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleCreditApprovedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditApprovedResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditApprovedUseCase;
import com.arbitrier.orchestrator.application.port.outbound.ConfirmOrderCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ConfirmOrderSagaCommand;
import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaCompletedDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use-case implementation: handle a CreditApproved event to complete the UC-01 saga.
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Load the existing saga.</li>
 *   <li>Record the credit approval via {@link Saga#creditApproved(String)}.</li>
 *   <li>Finalise via {@link Saga#complete()} (transitions to {@code COMPLETED}).</li>
 *   <li>Persist the completed saga.</li>
 *   <li>Publish {@link SagaCompletedDomainEvent}.</li>
 *   <li>Issue a {@link ConfirmOrderSagaCommand} to the order-service.</li>
 * </ol>
 *
 * <h2>Transactionality (deferred)</h2>
 * <p>This service will become {@code @Transactional} when JPA persistence is introduced.
 * DB + Kafka consistency will be handled by the Outbox pattern (ADR-0005).
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleCreditApprovedService implements HandleCreditApprovedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleCreditApprovedService.class);

    private final SagaRepository repository;
    private final SagaEventPublisher eventPublisher;
    private final ConfirmOrderCommandPublisher confirmOrderCommandPublisher;

    public HandleCreditApprovedService(
            final SagaRepository repository,
            final SagaEventPublisher eventPublisher,
            final ConfirmOrderCommandPublisher confirmOrderCommandPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.confirmOrderCommandPublisher = confirmOrderCommandPublisher;
    }

    @Override
    public HandleCreditApprovedResult handle(final HandleCreditApprovedCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        final Saga saga = loadSaga(sagaId);
        final Saga completed = saga.creditApproved(command.creditReservationId()).complete();

        repository.save(completed);
        publishSagaCompleted(completed);
        publishConfirmOrder(completed);

        log.info("CreditApproved handled sagaId={} orderId={} creditReservationId={} step={} status={}",
                sagaId, completed.orderId(), completed.creditReservationId(),
                completed.currentStep(), completed.status());

        return new HandleCreditApprovedResult(sagaId);
    }

    private Saga loadSaga(final SagaId sagaId) {
        return repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("No saga found with id: " + sagaId));
    }

    private void publishSagaCompleted(final Saga saga) {
        eventPublisher.publishCompleted(
                new SagaCompletedDomainEvent(saga.id(), saga.orderId()));
    }

    private void publishConfirmOrder(final Saga saga) {
        confirmOrderCommandPublisher.publishConfirmOrder(
                new ConfirmOrderSagaCommand(saga.id().value(), saga.orderId()));
    }
}
