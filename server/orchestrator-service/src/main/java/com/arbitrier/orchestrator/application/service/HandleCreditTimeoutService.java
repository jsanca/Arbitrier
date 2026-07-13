package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleCreditTimeoutCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditTimeoutResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditTimeoutUseCase;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockSagaCommand;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.CreditTimedOutDomainEvent;
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
 * Use-case implementation: handle a timeout of the credit reservation step.
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Load the existing saga (must be in {@code WAITING_FOR_CREDIT}).</li>
 *   <li>Execute {@link Saga#creditTimedOut()} — validates the saga is awaiting credit.</li>
 *   <li>Evaluate the retry policy via {@link CorporateBulkOrderSagaRetryPolicy#evaluateCredit(int)}.</li>
 *   <li>If {@link RetryDecision#RETRY}: call {@link Saga#retryCredit()}, persist, write
 *       {@link CreditTimedOutDomainEvent} to the outbox.</li>
 *   <li>If {@link RetryDecision#EXHAUST}: call {@link Saga#compensate()}, persist, write
 *       {@link SagaCompensatedDomainEvent} to the outbox, and issue
 *       {@link ReleaseStockSagaCommand} using the stored {@code stockReservationId} from
 *       the saga.</li>
 * </ol>
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleCreditTimeoutService implements HandleCreditTimeoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleCreditTimeoutService.class);
    private static final String AGGREGATE_TYPE = "Saga";

    private final SagaRepository repository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;
    private final ReleaseStockCommandPublisher releaseStockCommandPublisher;
    private final CorporateBulkOrderSagaRetryPolicy retryPolicy;

    public HandleCreditTimeoutService(
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
        outboxRepository.save(outboxMapper.map(
                new CreditTimedOutDomainEvent(saga.id(), saga.orderId(), attemptNumber),
                saga.id().value(),
                AGGREGATE_TYPE));
    }

    private void publishCompensated(final Saga saga) {
        outboxRepository.save(outboxMapper.map(
                new SagaCompensatedDomainEvent(saga.id(), saga.orderId()),
                saga.id().value(),
                AGGREGATE_TYPE));
    }

    private void publishReleaseStock(final Saga saga) {
        releaseStockCommandPublisher.publishReleaseStock(
                new ReleaseStockSagaCommand(
                        saga.id().value(), saga.stockReservationId(), saga.orderId()));
    }
}
