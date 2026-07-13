package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleStockReleasedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReleasedResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReleasedUseCase;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaCancelledDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.validation.Require;
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
 *   <li>Write {@link SagaCancelledDomainEvent} to the outbox.</li>
 * </ol>
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleStockReleasedService implements HandleStockReleasedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleStockReleasedService.class);
    private static final String AGGREGATE_TYPE = "Saga";

    private final SagaRepository repository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;

    public HandleStockReleasedService(
            final SagaRepository repository,
            final OutboxRepository outboxRepository,
            final DomainEventToOutboxMapper outboxMapper) {
        this.repository = Require.notNull(repository, "repository");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.outboxMapper = Require.notNull(outboxMapper, "outboxMapper");
    }

    @Override
    @Transactional
    public HandleStockReleasedResult handle(final HandleStockReleasedCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        final Saga saga = loadSaga(sagaId);
        final Saga cancelled = saga.inventoryReleased();

        repository.save(cancelled);
        outboxRepository.save(outboxMapper.map(
                new SagaCancelledDomainEvent(sagaId, saga.orderId()),
                sagaId.value(),
                AGGREGATE_TYPE));

        log.info("StockReleased handled sagaId={} orderId={} step={} status={}",
                sagaId, cancelled.orderId(), cancelled.currentStep(), cancelled.status());

        return new HandleStockReleasedResult(sagaId);
    }

    private Saga loadSaga(final SagaId sagaId) {
        return repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("No saga found with id: " + sagaId));
    }
}
