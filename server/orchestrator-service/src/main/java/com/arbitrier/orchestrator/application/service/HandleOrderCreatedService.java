package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleOrderCreatedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleOrderCreatedResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleOrderCreatedUseCase;
import com.arbitrier.orchestrator.application.port.outbound.ReserveStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveStockSagaCommand;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.command.SagaOrderLine;
import com.arbitrier.orchestrator.domain.event.SagaStartedDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Use-case implementation: handle an OrderCreated event to start the UC-01 saga.
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>If a saga with the same {@code sagaId} already exists, return early (idempotent).</li>
 *   <li>Create a new {@link Saga} at step {@code ORDER_CREATED}.</li>
 *   <li>Persist the saga.</li>
 *   <li>Write {@link SagaStartedDomainEvent} to the outbox.</li>
 *   <li>Issue a {@link ReserveStockSagaCommand} carrying the order lines to the
 *       inventory-service.</li>
 * </ol>
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleOrderCreatedService implements HandleOrderCreatedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleOrderCreatedService.class);
    private static final String AGGREGATE_TYPE = "Saga";

    private final SagaRepository repository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;
    private final ReserveStockCommandPublisher reserveStockCommandPublisher;

    public HandleOrderCreatedService(
            final SagaRepository repository,
            final OutboxRepository outboxRepository,
            final DomainEventToOutboxMapper outboxMapper,
            final ReserveStockCommandPublisher reserveStockCommandPublisher) {
        this.repository = Require.notNull(repository, "repository");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.outboxMapper = Require.notNull(outboxMapper, "outboxMapper");
        this.reserveStockCommandPublisher = Require.notNull(reserveStockCommandPublisher, "reserveStockCommandPublisher");
    }

    @Override
    @Transactional
    public HandleOrderCreatedResult handle(final HandleOrderCreatedCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        if (repository.findById(sagaId).isPresent()) {
            log.warn("Duplicate OrderCreated ignored sagaId={} orderId={}", sagaId, command.orderId());
            return new HandleOrderCreatedResult(sagaId, null);
        }

        final String stockReservationId = UUID.randomUUID().toString();
        final Saga saga = Saga.start(sagaId, command.orderId(), command.customerId())
                .awaitInventoryResponse();

        repository.save(saga);
        publishSagaStarted(saga);
        publishReserveStock(saga, stockReservationId, command.lines());

        log.info("OrderCreated handled sagaId={} orderId={} customerId={} step={} status={}",
                sagaId, command.orderId(), command.customerId(),
                saga.currentStep(), saga.status());

        return new HandleOrderCreatedResult(sagaId, stockReservationId);
    }

    private void publishSagaStarted(final Saga saga) {
        outboxRepository.save(outboxMapper.map(
                new SagaStartedDomainEvent(saga.id(), saga.orderId(), saga.customerId()),
                saga.id().value(),
                AGGREGATE_TYPE));
    }

    private void publishReserveStock(final Saga saga, final String stockReservationId,
                                      final List<SagaOrderLine> lines) {
        reserveStockCommandPublisher.publishReserveStock(
                new ReserveStockSagaCommand(saga.id().value(), stockReservationId, saga.orderId(), lines));
    }
}
