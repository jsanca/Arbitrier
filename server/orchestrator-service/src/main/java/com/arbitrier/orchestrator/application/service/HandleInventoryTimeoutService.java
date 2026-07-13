package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleInventoryTimeoutCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleInventoryTimeoutResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleInventoryTimeoutUseCase;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockSagaCommand;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.InventoryTimedOutDomainEvent;
import com.arbitrier.orchestrator.domain.event.SagaCompensatedDomainEvent;
import com.arbitrier.orchestrator.domain.model.CorporateBulkOrderSagaRetryPolicy;
import com.arbitrier.orchestrator.domain.model.RetryDecision;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case implementation: handle a timeout of the inventory reservation step.
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Load the existing saga (must be in {@code WAITING_FOR_INVENTORY}).</li>
 *   <li>Execute {@link Saga#inventoryTimedOut()} — validates the saga is awaiting inventory.</li>
 *   <li>Evaluate the retry policy via {@link CorporateBulkOrderSagaRetryPolicy#evaluateInventory(int)}.</li>
 *   <li>If {@link RetryDecision#RETRY}: call {@link Saga#retryInventory()}, persist, write
 *       {@link InventoryTimedOutDomainEvent} to the outbox.</li>
 *   <li>If {@link RetryDecision#EXHAUST}: call {@link Saga#compensate()}, persist, write
 *       {@link SagaCompensatedDomainEvent} to the outbox, and issue
 *       {@link ReleaseStockSagaCommand} using the reservation ID from the command.</li>
 * </ol>
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleInventoryTimeoutService implements HandleInventoryTimeoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleInventoryTimeoutService.class);
    private static final String AGGREGATE_TYPE = "Saga";

    private final SagaRepository repository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;
    private final ReleaseStockCommandPublisher releaseStockCommandPublisher;
    private final CorporateBulkOrderSagaRetryPolicy retryPolicy;

    public HandleInventoryTimeoutService(
            final SagaRepository repository,
            final OutboxRepository outboxRepository,
            final DomainEventToOutboxMapper outboxMapper,
            final ReleaseStockCommandPublisher releaseStockCommandPublisher,
            final CorporateBulkOrderSagaRetryPolicy retryPolicy) {
        this.repository = Require.notNull(repository, "repository");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.outboxMapper = Require.notNull(outboxMapper, "outboxMapper");
        this.releaseStockCommandPublisher = Require.notNull(releaseStockCommandPublisher, "releaseStockCommandPublisher");
        this.retryPolicy = Require.notNull(retryPolicy, "retryPolicy");
    }

    @Override
    @Transactional
    public HandleInventoryTimeoutResult handle(final HandleInventoryTimeoutCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());
        final Saga saga = loadSaga(sagaId);

        final Saga timedOut = saga.inventoryTimedOut();
        final RetryDecision decision = retryPolicy.evaluateInventory(command.attemptNumber());

        if (decision.shouldRetry()) {
            final Saga retrying = timedOut.retryInventory();
            repository.save(retrying);
            publishInventoryTimedOut(retrying, command.attemptNumber());
            log.info("Inventory timeout will retry sagaId={} orderId={} attempt={} maxAttempts={}",
                    sagaId, saga.orderId(), command.attemptNumber(), retryPolicy.inventoryMaxAttempts());
        } else {
            final Saga compensating = timedOut.compensate();
            repository.save(compensating);
            publishCompensated(compensating);
            publishReleaseStock(compensating, command.stockReservationId());
            log.info("Inventory timeout exhausted — compensating sagaId={} orderId={} attempt={} maxAttempts={}",
                    sagaId, saga.orderId(), command.attemptNumber(), retryPolicy.inventoryMaxAttempts());
        }

        return new HandleInventoryTimeoutResult(sagaId, decision);
    }

    private Saga loadSaga(final SagaId sagaId) {
        return repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("No saga found with id: " + sagaId));
    }

    private void publishInventoryTimedOut(final Saga saga, final int attemptNumber) {
        outboxRepository.save(outboxMapper.map(
                new InventoryTimedOutDomainEvent(saga.id(), saga.orderId(), attemptNumber),
                saga.id().value(),
                AGGREGATE_TYPE));
    }

    private void publishCompensated(final Saga saga) {
        outboxRepository.save(outboxMapper.map(
                new SagaCompensatedDomainEvent(saga.id(), saga.orderId()),
                saga.id().value(),
                AGGREGATE_TYPE));
    }

    private void publishReleaseStock(final Saga saga, final String stockReservationId) {
        releaseStockCommandPublisher.publishReleaseStock(
                new ReleaseStockSagaCommand(saga.id().value(), stockReservationId, saga.orderId()));
    }
}
