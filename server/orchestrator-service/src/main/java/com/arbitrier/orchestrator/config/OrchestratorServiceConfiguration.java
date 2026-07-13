package com.arbitrier.orchestrator.config;

import com.arbitrier.orchestrator.application.port.inbound.AdvanceSagaUseCase;
import com.arbitrier.orchestrator.application.port.inbound.CompensateSagaUseCase;
import com.arbitrier.orchestrator.application.port.inbound.HandleCompensationFailedUseCase;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditApprovedUseCase;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditRejectedUseCase;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditTimeoutUseCase;
import com.arbitrier.orchestrator.application.port.inbound.HandleInventoryTimeoutUseCase;
import com.arbitrier.orchestrator.application.port.inbound.HandleOrderCreatedUseCase;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockRejectedUseCase;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReleasedUseCase;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReservedUseCase;
import com.arbitrier.orchestrator.application.port.inbound.StartSagaUseCase;
import com.arbitrier.orchestrator.application.port.outbound.ConfirmOrderCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveCreditCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.application.service.AdvanceSagaService;
import com.arbitrier.orchestrator.application.service.CompensateSagaService;
import com.arbitrier.orchestrator.application.service.HandleCompensationFailedService;
import com.arbitrier.orchestrator.application.service.HandleCreditApprovedService;
import com.arbitrier.orchestrator.application.service.HandleCreditRejectedService;
import com.arbitrier.orchestrator.application.service.HandleCreditTimeoutService;
import com.arbitrier.orchestrator.application.service.HandleInventoryTimeoutService;
import com.arbitrier.orchestrator.application.service.HandleOrderCreatedService;
import com.arbitrier.orchestrator.application.service.HandleStockRejectedService;
import com.arbitrier.orchestrator.application.service.HandleStockReleasedService;
import com.arbitrier.orchestrator.application.service.HandleStockReservedService;
import com.arbitrier.orchestrator.application.service.StartSagaService;
import com.arbitrier.orchestrator.domain.model.CorporateBulkOrderSagaRetryPolicy;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring: binds application services to their outbound port implementations.
 *
 * <p>Layer: config
 * <p>Module: orchestrator-service
 */
@Configuration
public class OrchestratorServiceConfiguration {

    // ── ARB-014 foundation ────────────────────────────────────────────────────

    @Bean
    public StartSagaUseCase startSagaService(
            SagaRepository sagaRepository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper) {
        return new StartSagaService(sagaRepository, outboxRepository, outboxMapper);
    }

    @Bean
    public AdvanceSagaUseCase advanceSagaService(
            SagaRepository sagaRepository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper) {
        return new AdvanceSagaService(sagaRepository, outboxRepository, outboxMapper);
    }

    @Bean
    public CompensateSagaUseCase compensateSagaService(
            SagaRepository sagaRepository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper) {
        return new CompensateSagaService(sagaRepository, outboxRepository, outboxMapper);
    }

    // ── ARB-015 happy path ────────────────────────────────────────────────────

    @Bean
    public HandleOrderCreatedUseCase handleOrderCreatedService(
            SagaRepository sagaRepository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper,
            ReserveStockCommandPublisher reserveStockCommandPublisher) {
        return new HandleOrderCreatedService(sagaRepository, outboxRepository, outboxMapper,
                reserveStockCommandPublisher);
    }

    @Bean
    public HandleStockReservedUseCase handleStockReservedService(
            SagaRepository sagaRepository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper,
            ReserveCreditCommandPublisher reserveCreditCommandPublisher) {
        return new HandleStockReservedService(sagaRepository, outboxRepository, outboxMapper,
                reserveCreditCommandPublisher);
    }

    @Bean
    public HandleCreditApprovedUseCase handleCreditApprovedService(
            SagaRepository sagaRepository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper,
            ConfirmOrderCommandPublisher confirmOrderCommandPublisher) {
        return new HandleCreditApprovedService(sagaRepository, outboxRepository, outboxMapper,
                confirmOrderCommandPublisher);
    }

    // ── ARB-016 compensation ──────────────────────────────────────────────────

    @Bean
    public HandleStockRejectedUseCase handleStockRejectedService(
            SagaRepository sagaRepository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper) {
        return new HandleStockRejectedService(sagaRepository, outboxRepository, outboxMapper);
    }

    @Bean
    public HandleCreditRejectedUseCase handleCreditRejectedService(
            SagaRepository sagaRepository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper,
            ReleaseStockCommandPublisher releaseStockCommandPublisher) {
        return new HandleCreditRejectedService(sagaRepository, outboxRepository, outboxMapper,
                releaseStockCommandPublisher);
    }

    @Bean
    public HandleStockReleasedUseCase handleStockReleasedService(
            SagaRepository sagaRepository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper) {
        return new HandleStockReleasedService(sagaRepository, outboxRepository, outboxMapper);
    }

    @Bean
    public HandleCompensationFailedUseCase handleCompensationFailedService(
            SagaRepository sagaRepository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper) {
        return new HandleCompensationFailedService(sagaRepository, outboxRepository, outboxMapper);
    }

    // ── ARB-018 timeout policy ────────────────────────────────────────────────

    @Bean
    public CorporateBulkOrderSagaRetryPolicy corporateBulkOrderSagaRetryPolicy() {
        return new CorporateBulkOrderSagaRetryPolicy(3, 3);
    }

    @Bean
    public HandleInventoryTimeoutUseCase handleInventoryTimeoutService(
            SagaRepository sagaRepository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper,
            ReleaseStockCommandPublisher releaseStockCommandPublisher,
            CorporateBulkOrderSagaRetryPolicy retryPolicy) {
        return new HandleInventoryTimeoutService(
                sagaRepository, outboxRepository, outboxMapper, releaseStockCommandPublisher, retryPolicy);
    }

    @Bean
    public HandleCreditTimeoutUseCase handleCreditTimeoutService(
            SagaRepository sagaRepository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper,
            ReleaseStockCommandPublisher releaseStockCommandPublisher,
            CorporateBulkOrderSagaRetryPolicy retryPolicy) {
        return new HandleCreditTimeoutService(
                sagaRepository, outboxRepository, outboxMapper, releaseStockCommandPublisher, retryPolicy);
    }
}
