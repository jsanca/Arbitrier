package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.CompensateSagaCommand;
import com.arbitrier.orchestrator.application.port.inbound.CompensateSagaResult;
import com.arbitrier.orchestrator.application.port.inbound.CompensateSagaUseCase;
import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaCompensatedDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use-case implementation: begin compensation for a saga instance.
 *
 * <p>Loads the saga, transitions it to {@code COMPENSATING} via {@link Saga#compensate()},
 * persists the updated aggregate, and publishes {@link SagaCompensatedDomainEvent}.
 *
 * <p>This service only marks the saga as COMPENSATING. It does not issue any
 * compensating commands — that wiring belongs to ARB-016.
 *
 * <h2>Transactionality (deferred)</h2>
 * <p>This service will become {@code @Transactional} when JPA persistence is introduced.
 * DB + Kafka consistency will be handled by the Outbox pattern.
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class CompensateSagaService implements CompensateSagaUseCase {

    private static final Logger log = LoggerFactory.getLogger(CompensateSagaService.class);

    private final SagaRepository repository;
    private final SagaEventPublisher eventPublisher;

    public CompensateSagaService(SagaRepository repository, SagaEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public CompensateSagaResult compensate(final CompensateSagaCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        final Saga saga = repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No saga found with id: " + sagaId));

        final Saga compensating = saga.compensate();

        repository.save(compensating);
        eventPublisher.publishCompensated(
                new SagaCompensatedDomainEvent(sagaId, saga.orderId()));

        log.info("Saga compensation started sagaId={} orderId={} step={} status={}",
                sagaId, saga.orderId(), compensating.currentStep(), compensating.status());

        return new CompensateSagaResult(sagaId);
    }
}
