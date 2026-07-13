package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.CompensateSagaCommand;
import com.arbitrier.orchestrator.application.port.inbound.CompensateSagaResult;
import com.arbitrier.orchestrator.application.port.inbound.CompensateSagaUseCase;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaCompensatedDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case implementation: begin compensation for a saga instance.
 *
 * <p>Loads the saga, transitions it to {@code COMPENSATING} via {@link Saga#compensate()},
 * persists the updated aggregate, and writes {@link SagaCompensatedDomainEvent} to the outbox.
 *
 * <p>This service only marks the saga as COMPENSATING. It does not issue any
 * compensating commands — that wiring belongs to ARB-016.
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class CompensateSagaService implements CompensateSagaUseCase {

    private static final Logger log = LoggerFactory.getLogger(CompensateSagaService.class);
    private static final String AGGREGATE_TYPE = "Saga";

    private final SagaRepository repository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;

    public CompensateSagaService(SagaRepository repository,
                                  OutboxRepository outboxRepository,
                                  DomainEventToOutboxMapper outboxMapper) {
        this.repository = Require.notNull(repository, "repository");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.outboxMapper = Require.notNull(outboxMapper, "outboxMapper");
    }

    @Override
    @Transactional
    public CompensateSagaResult compensate(final CompensateSagaCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        final Saga saga = repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No saga found with id: " + sagaId));

        final Saga compensating = saga.compensate();

        repository.save(compensating);
        outboxRepository.save(outboxMapper.map(
                new SagaCompensatedDomainEvent(sagaId, saga.orderId()),
                sagaId.value(),
                AGGREGATE_TYPE));

        log.info("Saga compensation started sagaId={} orderId={} step={} status={}",
                sagaId, saga.orderId(), compensating.currentStep(), compensating.status());

        return new CompensateSagaResult(sagaId);
    }
}
