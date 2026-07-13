package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.StartSagaCommand;
import com.arbitrier.orchestrator.application.port.inbound.StartSagaResult;
import com.arbitrier.orchestrator.application.port.inbound.StartSagaUseCase;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaStartedDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case implementation: start a new saga instance for a placed order.
 *
 * <p>Creates the {@link Saga} aggregate in the {@code STARTED} status at step
 * {@code ORDER_CREATED}, persists it, and writes {@link SagaStartedDomainEvent} to the outbox.
 *
 * <p>OPEN QUESTION: Duplicate saga detection — if a saga with the same {@code sagaId}
 * already exists, this service will overwrite it. An idempotency check should be
 * added at the Kafka consumer layer when ARB-015 wires the consumer.
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class StartSagaService implements StartSagaUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartSagaService.class);
    private static final String AGGREGATE_TYPE = "Saga";

    private final SagaRepository repository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;

    public StartSagaService(SagaRepository repository,
                             OutboxRepository outboxRepository,
                             DomainEventToOutboxMapper outboxMapper) {
        this.repository = Require.notNull(repository, "repository");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.outboxMapper = Require.notNull(outboxMapper, "outboxMapper");
    }

    @Override
    @Transactional
    public StartSagaResult start(final StartSagaCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        final Saga saga = Saga.start(sagaId, command.orderId(), command.customerId());

        repository.save(saga);
        outboxRepository.save(outboxMapper.map(
                new SagaStartedDomainEvent(sagaId, command.orderId(), command.customerId()),
                sagaId.value(),
                AGGREGATE_TYPE));

        log.info("Saga started sagaId={} orderId={} step={}",
                sagaId, command.orderId(), saga.currentStep());

        return new StartSagaResult(sagaId);
    }
}
