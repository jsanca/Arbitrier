package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleStockReleasedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReleasedResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReleasedUseCase;
import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaCancelledDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case implementation: handle a StockReleased event after inventory compensation.
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Load the existing saga (expected to be {@code COMPENSATING}).</li>
 *   <li>Cancel via {@link Saga#inventoryReleased()} — compensation is complete.</li>
 *   <li>Persist the cancelled saga.</li>
 *   <li>Publish {@link SagaCancelledDomainEvent}.</li>
 * </ol>
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleStockReleasedService implements HandleStockReleasedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleStockReleasedService.class);

    private final SagaRepository repository;
    private final SagaEventPublisher eventPublisher;

    public HandleStockReleasedService(
            final SagaRepository repository,
            final SagaEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public HandleStockReleasedResult handle(final HandleStockReleasedCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        final Saga saga = loadSaga(sagaId);
        final Saga cancelled = saga.inventoryReleased();

        repository.save(cancelled);
        eventPublisher.publishCancelled(new SagaCancelledDomainEvent(sagaId, saga.orderId()));

        log.info("StockReleased handled sagaId={} orderId={} step={} status={}",
                sagaId, cancelled.orderId(), cancelled.currentStep(), cancelled.status());

        return new HandleStockReleasedResult(sagaId);
    }

    private Saga loadSaga(final SagaId sagaId) {
        return repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("No saga found with id: " + sagaId));
    }
}
