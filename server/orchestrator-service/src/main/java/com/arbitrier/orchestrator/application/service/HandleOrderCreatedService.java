package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleOrderCreatedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleOrderCreatedResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleOrderCreatedUseCase;
import com.arbitrier.orchestrator.application.port.outbound.ReserveStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveStockSagaCommand;
import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaStartedDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Use-case implementation: handle an OrderCreated event to start the UC-01 saga.
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Create a new {@link Saga} at step {@code ORDER_CREATED}.</li>
 *   <li>Persist the saga.</li>
 *   <li>Publish {@link SagaStartedDomainEvent}.</li>
 *   <li>Issue a {@link ReserveStockSagaCommand} to the inventory-service.</li>
 * </ol>
 *
 * <p>OPEN QUESTION: Idempotency — a second call with the same {@code sagaId} will overwrite
 * the existing saga. A duplicate-detection check should be added when the Kafka consumer
 * is wired (post ARB-015).
 *
 * <h2>Transactionality (deferred)</h2>
 * <p>This service will become {@code @Transactional} when JPA persistence is introduced.
 * DB + Kafka consistency will be handled by the Outbox pattern (ADR-0005).
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleOrderCreatedService implements HandleOrderCreatedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleOrderCreatedService.class);

    private final SagaRepository repository;
    private final SagaEventPublisher eventPublisher;
    private final ReserveStockCommandPublisher reserveStockCommandPublisher;

    public HandleOrderCreatedService(
            final SagaRepository repository,
            final SagaEventPublisher eventPublisher,
            final ReserveStockCommandPublisher reserveStockCommandPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.reserveStockCommandPublisher = reserveStockCommandPublisher;
    }

    @Override
    public HandleOrderCreatedResult handle(final HandleOrderCreatedCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());
        final String stockReservationId = UUID.randomUUID().toString();

        final Saga saga = Saga.start(sagaId, command.orderId(), command.customerId());

        repository.save(saga);
        publishSagaStarted(saga);
        publishReserveStock(saga, stockReservationId);

        log.info("OrderCreated handled sagaId={} orderId={} customerId={} step={} status={}",
                sagaId, command.orderId(), command.customerId(),
                saga.currentStep(), saga.status());

        return new HandleOrderCreatedResult(sagaId, stockReservationId);
    }

    private void publishSagaStarted(final Saga saga) {
        eventPublisher.publishStarted(
                new SagaStartedDomainEvent(saga.id(), saga.orderId(), saga.customerId()));
    }

    private void publishReserveStock(final Saga saga, final String stockReservationId) {
        reserveStockCommandPublisher.publishReserveStock(
                new ReserveStockSagaCommand(saga.id().value(), stockReservationId, saga.orderId()));
    }
}
