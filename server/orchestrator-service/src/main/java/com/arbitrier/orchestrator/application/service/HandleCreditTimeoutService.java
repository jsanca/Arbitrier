package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleCreditTimeoutCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditTimeoutResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditTimeoutUseCase;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockSagaCommand;
import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.CreditTimedOutDomainEvent;
import com.arbitrier.orchestrator.domain.event.SagaCompensatedDomainEvent;
import com.arbitrier.orchestrator.domain.model.CorporateBulkOrderSagaRetryPolicy;
import com.arbitrier.orchestrator.domain.model.RetryDecision;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case implementation: handle a timeout of the credit reservation step.
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Load the existing saga (must be in {@code WAITING_FOR_CREDIT}).</li>
 *   <li>Execute {@link Saga#creditTimedOut()} — validates the saga is awaiting credit.</li>
 *   <li>Evaluate the retry policy via {@link CorporateBulkOrderSagaRetryPolicy#evaluateCredit(int)}.</li>
 *   <li>If {@link RetryDecision#RETRY}: call {@link Saga#retryCredit()}, persist, publish
 *       {@link CreditTimedOutDomainEvent}.</li>
 *   <li>If {@link RetryDecision#EXHAUST}: call {@link Saga#compensate()}, persist, publish
 *       {@link SagaCompensatedDomainEvent}, issue {@link ReleaseStockSagaCommand} using the
 *       stored {@code stockReservationId} from the saga. Inventory was successfully reserved
 *       before the credit step began, so compensation must release it.</li>
 * </ol>
 *
 * <p>No scheduler, sleep, or retry execution is performed here. On retry, the runtime
 * infrastructure (a future slice) will re-issue the ReserveCredit command on receipt of
 * {@link CreditTimedOutDomainEvent}.
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleCreditTimeoutService implements HandleCreditTimeoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleCreditTimeoutService.class);

    private final SagaRepository repository;
    private final SagaEventPublisher eventPublisher;
    private final ReleaseStockCommandPublisher releaseStockCommandPublisher;
    private final CorporateBulkOrderSagaRetryPolicy retryPolicy;

    public HandleCreditTimeoutService(
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
    public HandleCreditTimeoutResult handle(final HandleCreditTimeoutCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());
        final Saga saga = loadSaga(sagaId);

        final Saga timedOut = saga.creditTimedOut();
        final RetryDecision decision = retryPolicy.evaluateCredit(command.attemptNumber());

        if (decision.shouldRetry()) {
            final Saga retrying = timedOut.retryCredit();
            repository.save(retrying);
            publishCreditTimedOut(retrying, command.attemptNumber());
            log.info("Credit timeout will retry sagaId={} orderId={} attempt={} maxAttempts={}",
                    sagaId, saga.orderId(), command.attemptNumber(), retryPolicy.creditMaxAttempts());
        } else {
            final Saga compensating = timedOut.compensate();
            repository.save(compensating);
            publishCompensated(compensating);
            publishReleaseStock(compensating);
            log.info("Credit timeout exhausted — compensating sagaId={} orderId={} attempt={} maxAttempts={}",
                    sagaId, saga.orderId(), command.attemptNumber(), retryPolicy.creditMaxAttempts());
        }

        return new HandleCreditTimeoutResult(sagaId, decision);
    }

    private Saga loadSaga(final SagaId sagaId) {
        return repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("No saga found with id: " + sagaId));
    }

    private void publishCreditTimedOut(final Saga saga, final int attemptNumber) {
        eventPublisher.publishCreditTimedOut(
                new CreditTimedOutDomainEvent(saga.id(), saga.orderId(), attemptNumber));
    }

    private void publishCompensated(final Saga saga) {
        eventPublisher.publishCompensated(
                new SagaCompensatedDomainEvent(saga.id(), saga.orderId()));
    }

    private void publishReleaseStock(final Saga saga) {
        releaseStockCommandPublisher.publishReleaseStock(
                new ReleaseStockSagaCommand(
                        saga.id().value(), saga.stockReservationId(), saga.orderId()));
    }
}
