package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleStockReservedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReservedResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReservedUseCase;
import com.arbitrier.orchestrator.application.port.outbound.ReserveCreditCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveCreditSagaCommand;
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

import java.util.UUID;

/**
 * Use-case implementation: handle a StockReserved event to advance the UC-01 saga.
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Load the existing saga.</li>
 *   <li>Record the stock reservation via {@link Saga#inventoryReserved(String)}
 *       (advances step to {@code VALIDATE_CREDIT}).</li>
 *   <li>Persist the updated saga.</li>
 *   <li>Write {@link SagaAdvancedDomainEvent} to the outbox.</li>
 *   <li>Issue a {@link ReserveCreditSagaCommand} to the credit-service.</li>
 * </ol>
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleStockReservedService implements HandleStockReservedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleStockReservedService.class);
    private static final String AGGREGATE_TYPE = "Saga";

    private final SagaRepository repository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;
    private final ReserveCreditCommandPublisher reserveCreditCommandPublisher;

    public HandleStockReservedService(
            final SagaRepository repository,
            final OutboxRepository outboxRepository,
            final DomainEventToOutboxMapper outboxMapper,
            final ReserveCreditCommandPublisher reserveCreditCommandPublisher) {
        this.repository = Require.notNull(repository, "repository");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.outboxMapper = Require.notNull(outboxMapper, "outboxMapper");
        this.reserveCreditCommandPublisher = Require.notNull(reserveCreditCommandPublisher, "reserveCreditCommandPublisher");
    }

    @Override
    @Transactional
    public HandleStockReservedResult handle(final HandleStockReservedCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());
        final String creditReservationId = UUID.randomUUID().toString();

        final Saga saga = loadSaga(sagaId);
        final Saga advanced = saga.inventoryReserved(command.stockReservationId())
                .awaitCreditResponse();

        repository.save(advanced);
        publishSagaAdvanced(advanced);
        publishReserveCredit(advanced, creditReservationId);

        log.info("StockReserved handled sagaId={} orderId={} stockReservationId={} step={} status={}",
                sagaId, advanced.orderId(), advanced.stockReservationId(),
                advanced.currentStep(), advanced.status());

        return new HandleStockReservedResult(sagaId, creditReservationId);
    }

    private Saga loadSaga(final SagaId sagaId) {
        return repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("No saga found with id: " + sagaId));
    }

    private void publishSagaAdvanced(final Saga saga) {
        outboxRepository.save(outboxMapper.map(
                new SagaAdvancedDomainEvent(saga.id(), saga.orderId(), saga.currentStep()),
                saga.id().value(),
                AGGREGATE_TYPE));
    }

    private void publishReserveCredit(final Saga saga, final String creditReservationId) {
        reserveCreditCommandPublisher.publishReserveCredit(
                new ReserveCreditSagaCommand(
                        saga.id().value(), creditReservationId, saga.orderId(), saga.customerId()));
    }
}
