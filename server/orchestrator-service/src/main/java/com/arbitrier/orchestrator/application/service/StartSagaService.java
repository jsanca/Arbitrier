package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.StartSagaCommand;
import com.arbitrier.orchestrator.application.port.inbound.StartSagaResult;
import com.arbitrier.orchestrator.application.port.inbound.StartSagaUseCase;
import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaStartedDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use-case implementation: start a new saga instance for a placed order.
 *
 * <p>Creates the {@link Saga} aggregate in the {@code STARTED} status at step
 * {@code ORDER_CREATED}, persists it, and publishes {@link SagaStartedDomainEvent}.
 *
 * <p>OPEN QUESTION: Duplicate saga detection — if a saga with the same {@code sagaId}
 * already exists, this service will overwrite it. An idempotency check should be
 * added at the Kafka consumer layer when ARB-015 wires the consumer.
 *
 * <h2>Transactionality (deferred)</h2>
 * <p>This service will become {@code @Transactional} when JPA persistence is introduced.
 * DB + Kafka consistency will be handled by the Outbox pattern.
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class StartSagaService implements StartSagaUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartSagaService.class);

    private final SagaRepository repository;
    private final SagaEventPublisher eventPublisher;

    public StartSagaService(SagaRepository repository, SagaEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public StartSagaResult start(final StartSagaCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        final Saga saga = Saga.start(sagaId, command.orderId(), command.customerId());

        repository.save(saga);
        eventPublisher.publishStarted(
                new SagaStartedDomainEvent(sagaId, command.orderId(), command.customerId()));

        log.info("Saga started sagaId={} orderId={} step={}",
                sagaId, command.orderId(), saga.currentStep());

        return new StartSagaResult(sagaId);
    }
}
