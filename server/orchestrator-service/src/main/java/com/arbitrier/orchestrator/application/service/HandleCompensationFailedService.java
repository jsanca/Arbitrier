package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleCompensationFailedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleCompensationFailedResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleCompensationFailedUseCase;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaCompensationFailedDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case implementation: handle a compensation failure.
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Load the existing saga.</li>
 *   <li>Transition to {@code FAILED_COMPENSATION} via {@link Saga#failCompensation()}.</li>
 *   <li>Persist the failed saga.</li>
 *   <li>Write {@link SagaCompensationFailedDomainEvent} to the outbox.</li>
 * </ol>
 *
 * <p>{@code FAILED_COMPENSATION} is a terminal state requiring manual intervention.
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleCompensationFailedService implements HandleCompensationFailedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleCompensationFailedService.class);
    private static final String AGGREGATE_TYPE = "Saga";

    private final SagaRepository repository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;

    public HandleCompensationFailedService(
            final SagaRepository repository,
            final OutboxRepository outboxRepository,
            final DomainEventToOutboxMapper outboxMapper) {
        this.repository = Require.notNull(repository, "repository");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.outboxMapper = Require.notNull(outboxMapper, "outboxMapper");
    }

    @Override
    @Transactional
    public HandleCompensationFailedResult handle(final HandleCompensationFailedCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        final Saga saga = loadSaga(sagaId);
        final Saga failed = saga.failCompensation();

        repository.save(failed);
        outboxRepository.save(outboxMapper.map(
                new SagaCompensationFailedDomainEvent(sagaId, saga.orderId()),
                sagaId.value(),
                AGGREGATE_TYPE));

        log.warn("Compensation failed sagaId={} orderId={} step={} status={}",
                sagaId, failed.orderId(), failed.currentStep(), failed.status());

        return new HandleCompensationFailedResult(sagaId);
    }

    private Saga loadSaga(final SagaId sagaId) {
        return repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("No saga found with id: " + sagaId));
    }
}
