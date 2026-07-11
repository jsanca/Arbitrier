package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.HandleStockReservedCommand;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReservedResult;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReservedUseCase;
import com.arbitrier.orchestrator.application.port.outbound.ReserveCreditCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveCreditSagaCommand;
import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaAdvancedDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
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
 *   <li>Publish {@link SagaAdvancedDomainEvent}.</li>
 *   <li>Issue a {@link ReserveCreditSagaCommand} to the credit-service.</li>
 * </ol>
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class HandleStockReservedService implements HandleStockReservedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleStockReservedService.class);

    private final SagaRepository repository;
    private final SagaEventPublisher eventPublisher;
    private final ReserveCreditCommandPublisher reserveCreditCommandPublisher;

    public HandleStockReservedService(
            final SagaRepository repository,
            final SagaEventPublisher eventPublisher,
            final ReserveCreditCommandPublisher reserveCreditCommandPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.reserveCreditCommandPublisher = reserveCreditCommandPublisher;
    }

    @Override
    @Transactional
    public HandleStockReservedResult handle(final HandleStockReservedCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());
        final String creditReservationId = UUID.randomUUID().toString();

        final Saga saga = loadSaga(sagaId);
        // TODO: When Kafka wiring is introduced, validate that command.stockReservationId()
        // matches the reservation ID originally issued in ReserveStockSagaCommand. This guards
        // against message-ordering issues where a reply from a stale or different reservation arrives.
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
        eventPublisher.publishAdvanced(
                new SagaAdvancedDomainEvent(saga.id(), saga.orderId(), saga.currentStep()));
    }

    private void publishReserveCredit(final Saga saga, final String creditReservationId) {
        reserveCreditCommandPublisher.publishReserveCredit(
                new ReserveCreditSagaCommand(
                        saga.id().value(), creditReservationId, saga.orderId(), saga.customerId()));
    }
}
