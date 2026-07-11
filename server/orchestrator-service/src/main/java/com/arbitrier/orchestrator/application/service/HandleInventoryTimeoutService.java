package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleInventoryTimeoutCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleInventoryTimeoutResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleInventoryTimeoutUseCase;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockSagaCommand;
import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.InventoryTimedOutDomainEvent;
import com.arbitrier.orchestrator.domain.event.SagaCompensatedDomainEvent;
import com.arbitrier.orchestrator.domain.model.CorporateBulkOrderSagaRetryPolicy;
import com.arbitrier.orchestrator.domain.model.RetryDecision;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
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
 *   <li>If {@link RetryDecision#RETRY}: call {@link Saga#retryInventory()}, persist, publish
 *       {@link InventoryTimedOutDomainEvent}.</li>
 *   <li>If {@link RetryDecision#EXHAUST}: call {@link Saga#compensate()}, persist, publish
 *       {@link SagaCompensatedDomainEvent}, issue {@link ReleaseStockSagaCommand} using the
 *       reservation ID from the command. The release is idempotent — if inventory-service
 *       never processed the reservation, the release is a safe no-op.</li>
 * </ol>
 *
 * <p>No scheduler, sleep, or retry execution is performed here. On retry, the runtime
 * infrastructure (a future slice) will re-issue the ReserveStock command on receipt of
 * {@link InventoryTimedOutDomainEvent}.
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleInventoryTimeoutService implements HandleInventoryTimeoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleInventoryTimeoutService.class);

    private final SagaRepository repository;
    private final SagaEventPublisher eventPublisher;
    private final ReleaseStockCommandPublisher releaseStockCommandPublisher;
    private final CorporateBulkOrderSagaRetryPolicy retryPolicy;

    public HandleInventoryTimeoutService(
            final SagaRepository repository,
            final SagaEventPublisher eventPublisher,
            final ReleaseStockCommandPublisher releaseStockCommandPublisher,
            final CorporateBulkOrderSagaRetryPolicy retryPolicy) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.releaseStockCommandPublisher = releaseStockCommandPublisher;
        this.retryPolicy = retryPolicy;
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
        eventPublisher.publishInventoryTimedOut(
                new InventoryTimedOutDomainEvent(saga.id(), saga.orderId(), attemptNumber));
    }

    private void publishCompensated(final Saga saga) {
        eventPublisher.publishCompensated(
                new SagaCompensatedDomainEvent(saga.id(), saga.orderId()));
    }

    private void publishReleaseStock(final Saga saga, final String stockReservationId) {
        releaseStockCommandPublisher.publishReleaseStock(
                new ReleaseStockSagaCommand(saga.id().value(), stockReservationId, saga.orderId()));
    }
}
