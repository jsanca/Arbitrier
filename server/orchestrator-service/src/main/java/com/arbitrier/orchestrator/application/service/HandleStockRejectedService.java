package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleStockRejectedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockRejectedResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockRejectedUseCase;
import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaCancelledDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case implementation: handle a StockRejected event.
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Load the existing saga.</li>
 *   <li>Cancel via {@link Saga#stockRejected()} — no inventory was reserved;
 *       no compensation command is needed.</li>
 *   <li>Persist the cancelled saga.</li>
 *   <li>Publish {@link SagaCancelledDomainEvent}.</li>
 * </ol>
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleStockRejectedService implements HandleStockRejectedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleStockRejectedService.class);

    private final SagaRepository repository;
    private final SagaEventPublisher eventPublisher;

    public HandleStockRejectedService(
            final SagaRepository repository,
            final SagaEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public HandleStockRejectedResult handle(final HandleStockRejectedCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        final Saga saga = loadSaga(sagaId);
        final Saga cancelled = saga.stockRejected();

        repository.save(cancelled);
        eventPublisher.publishCancelled(new SagaCancelledDomainEvent(sagaId, saga.orderId()));

        log.info("StockRejected handled sagaId={} orderId={} step={} status={}",
                sagaId, cancelled.orderId(), cancelled.currentStep(), cancelled.status());

        return new HandleStockRejectedResult(sagaId);
    }

    private Saga loadSaga(final SagaId sagaId) {
        return repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("No saga found with id: " + sagaId));
    }
}
