package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleCreditApprovedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditApprovedResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditApprovedUseCase;
import com.arbitrier.orchestrator.application.port.outbound.ConfirmOrderCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ConfirmOrderSagaCommand;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaCompletedDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case implementation: handle a CreditApproved event to complete the UC-01 saga.
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Load the existing saga.</li>
 *   <li>Record the credit approval via {@link Saga#creditApproved(String)}.</li>
 *   <li>Finalise via {@link Saga#complete()} (transitions to {@code COMPLETED}).</li>
 *   <li>Persist the completed saga.</li>
 *   <li>Write {@link SagaCompletedDomainEvent} to the outbox.</li>
 *   <li>Issue a {@link ConfirmOrderSagaCommand} to the order-service.</li>
 * </ol>
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleCreditApprovedService implements HandleCreditApprovedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleCreditApprovedService.class);
    private static final String AGGREGATE_TYPE = "Saga";

    private final SagaRepository repository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;
    private final ConfirmOrderCommandPublisher confirmOrderCommandPublisher;

    public HandleCreditApprovedService(
            final SagaRepository repository,
            final OutboxRepository outboxRepository,
            final DomainEventToOutboxMapper outboxMapper,
            final ConfirmOrderCommandPublisher confirmOrderCommandPublisher) {
        this.repository = Require.notNull(repository, "repository");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.outboxMapper = Require.notNull(outboxMapper, "outboxMapper");
        this.confirmOrderCommandPublisher = Require.notNull(confirmOrderCommandPublisher, "confirmOrderCommandPublisher");
    }

    @Override
    @Transactional
    public HandleCreditApprovedResult handle(final HandleCreditApprovedCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        final Saga saga = loadSaga(sagaId);
        requireInventoryReserved(saga);
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

    private void requireInventoryReserved(final Saga saga) {
        if (saga.stockReservationId() == null) {
            throw new IllegalArgumentException(
                    "Saga sagaId=" + saga.id() + " has not yet recorded a stock reservation" +
                    " — creditApproved requires inventory to be reserved first");
        }
    }

    private void publishSagaCompleted(final Saga saga) {
        outboxRepository.save(outboxMapper.map(
                new SagaCompletedDomainEvent(saga.id(), saga.orderId()),
                saga.id().value(),
                AGGREGATE_TYPE));
    }

    private void publishConfirmOrder(final Saga saga) {
        confirmOrderCommandPublisher.publishConfirmOrder(
                new ConfirmOrderSagaCommand(saga.id().value(), saga.orderId()));
    }
}
