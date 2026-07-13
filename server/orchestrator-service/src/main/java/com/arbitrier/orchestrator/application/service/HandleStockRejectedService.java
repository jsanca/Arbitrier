package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleStockRejectedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockRejectedResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockRejectedUseCase;
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
 * Use-case implementation: handle a StockRejected event.
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Load the existing saga.</li>
 *   <li>Cancel via {@link Saga#stockRejected()} — no inventory was reserved;
 *       no compensation command is needed.</li>
 *   <li>Persist the cancelled saga.</li>
 *   <li>Write {@link SagaCancelledDomainEvent} to the outbox.</li>
 * </ol>
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleStockRejectedService implements HandleStockRejectedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleStockRejectedService.class);
    private static final String AGGREGATE_TYPE = "Saga";

    private final SagaRepository repository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;

    public HandleStockRejectedService(
            final SagaRepository repository,
            final OutboxRepository outboxRepository,
            final DomainEventToOutboxMapper outboxMapper) {
        this.repository = Require.notNull(repository, "repository");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.outboxMapper = Require.notNull(outboxMapper, "outboxMapper");
    }

    @Override
    @Transactional
    public HandleStockRejectedResult handle(final HandleStockRejectedCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        final Saga saga = loadSaga(sagaId);
        final Saga cancelled = saga.stockRejected();

        repository.save(cancelled);
        outboxRepository.save(outboxMapper.map(
                new SagaCancelledDomainEvent(sagaId, saga.orderId()),
                sagaId.value(),
                AGGREGATE_TYPE));

        log.info("StockRejected handled sagaId={} orderId={} step={} status={}",
                sagaId, cancelled.orderId(), cancelled.currentStep(), cancelled.status());

        return new HandleStockRejectedResult(sagaId);
    }

    private Saga loadSaga(final SagaId sagaId) {
        return repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("No saga found with id: " + sagaId));
    }
}
