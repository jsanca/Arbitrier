package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleCreditRejectedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditRejectedResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditRejectedUseCase;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockSagaCommand;
import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaCompensatedDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case implementation: handle a CreditRejected event.
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Load the existing saga.</li>
 *   <li>Begin compensation via {@link Saga#creditRejected()} — transitions to
 *       {@code COMPENSATING}, step {@code COMPENSATE_INVENTORY}. Requires a stored
 *       {@code stockReservationId}.</li>
 *   <li>Persist the compensating saga.</li>
 *   <li>Publish {@link SagaCompensatedDomainEvent}.</li>
 *   <li>Issue a {@link ReleaseStockSagaCommand} to the inventory-service.</li>
 * </ol>
 *
 * <p>Credit is NOT released — it was never approved.
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleCreditRejectedService implements HandleCreditRejectedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleCreditRejectedService.class);

    private final SagaRepository repository;
    private final SagaEventPublisher eventPublisher;
    private final ReleaseStockCommandPublisher releaseStockCommandPublisher;

    public HandleCreditRejectedService(
            final SagaRepository repository,
            final SagaEventPublisher eventPublisher,
            final ReleaseStockCommandPublisher releaseStockCommandPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.releaseStockCommandPublisher = releaseStockCommandPublisher;
    }

    @Override
    @Transactional
    public HandleCreditRejectedResult handle(final HandleCreditRejectedCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        final Saga saga = loadSaga(sagaId);
        final Saga compensating = saga.creditRejected();

        repository.save(compensating);
        publishSagaCompensated(compensating);
        publishReleaseStock(compensating);

        log.info("CreditRejected handled sagaId={} orderId={} stockReservationId={} step={} status={}",
                sagaId, compensating.orderId(), compensating.stockReservationId(),
                compensating.currentStep(), compensating.status());

        return new HandleCreditRejectedResult(sagaId);
    }

    private Saga loadSaga(final SagaId sagaId) {
        return repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("No saga found with id: " + sagaId));
    }

    private void publishSagaCompensated(final Saga saga) {
        eventPublisher.publishCompensated(
                new SagaCompensatedDomainEvent(saga.id(), saga.orderId()));
    }

    private void publishReleaseStock(final Saga saga) {
        releaseStockCommandPublisher.publishReleaseStock(
                new ReleaseStockSagaCommand(
                        saga.id().value(), saga.stockReservationId(), saga.orderId()));
    }
}
