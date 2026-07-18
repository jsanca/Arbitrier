package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.AdvanceSagaCommand;
import com.arbitrier.orchestrator.application.port.inbound.AdvanceSagaResult;
import com.arbitrier.orchestrator.application.port.inbound.AdvanceSagaUseCase;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaAdvancedDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case implementation: advance a saga to a new processing step.
 *
 * <p>Loads the saga, calls {@link Saga#advance(com.arbitrier.orchestrator.domain.model.SagaStep)},
 * persists the updated aggregate, and writes {@link SagaAdvancedDomainEvent} to the outbox.
 *
 * <p>Business step-sequencing logic (which step follows which) belongs to ARB-015.
 * This service is a general-purpose step-transition mechanism.
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class AdvanceSagaService implements AdvanceSagaUseCase {

    private static final Logger log = LoggerFactory.getLogger(AdvanceSagaService.class);
    private static final String AGGREGATE_TYPE = "Saga";

    private final SagaRepository repository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;

    public AdvanceSagaService(SagaRepository repository,
                               OutboxRepository outboxRepository,
                               DomainEventToOutboxMapper outboxMapper) {
        this.repository = Require.notNull(repository, "repository");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.outboxMapper = Require.notNull(outboxMapper, "outboxMapper");
    }

    @Override
    @Transactional
    public AdvanceSagaResult advance(final AdvanceSagaCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        final Saga saga = repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No saga found with id: " + sagaId));

        // #revision-advise [Concurrency & Race Conditions]
        // CRITICAL: saga.advance() yields a brand new immutable Saga instance.
        // However, if multiple Kafka listeners (e.g., credit and inventory responses)
        // trigger this service concurrently for the same sagaId, we are exposed to a
        // "Lost Update" anomaly at the database level.
        // TO DO / TO VERIFY: Ensure the underlying Saga persistence entity implements
        // Optimistic Locking (e.g., JPA @Version) or State-Based Constraints to guarantee
        // that a transaction fails-fast if another concurrent worker advanced the saga first.
        final Saga advanced = saga.advance(command.nextStep());

        repository.save(advanced);
        outboxRepository.save(outboxMapper.map(
                new SagaAdvancedDomainEvent(sagaId, saga.orderId(), advanced.currentStep()),
                sagaId.value(),
                AGGREGATE_TYPE));

        log.info("Saga advanced sagaId={} orderId={} step={} status={}",
                sagaId, saga.orderId(), advanced.currentStep(), advanced.status());

        return new AdvanceSagaResult(sagaId, advanced.currentStep());
    }
}
