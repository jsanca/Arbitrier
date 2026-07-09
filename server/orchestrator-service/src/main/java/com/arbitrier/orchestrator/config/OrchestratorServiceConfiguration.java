package com.arbitrier.orchestrator.config;

import com.arbitrier.orchestrator.application.port.inbound.AdvanceSagaUseCase;
import com.arbitrier.orchestrator.application.port.inbound.CompensateSagaUseCase;
import com.arbitrier.orchestrator.application.port.inbound.HandleCreditApprovedUseCase;
import com.arbitrier.orchestrator.application.port.inbound.HandleOrderCreatedUseCase;
import com.arbitrier.orchestrator.application.port.inbound.HandleStockReservedUseCase;
import com.arbitrier.orchestrator.application.port.inbound.StartSagaUseCase;
import com.arbitrier.orchestrator.application.port.outbound.ConfirmOrderCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveCreditCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.application.service.AdvanceSagaService;
import com.arbitrier.orchestrator.application.service.CompensateSagaService;
import com.arbitrier.orchestrator.application.service.HandleCreditApprovedService;
import com.arbitrier.orchestrator.application.service.HandleOrderCreatedService;
import com.arbitrier.orchestrator.application.service.HandleStockReservedService;
import com.arbitrier.orchestrator.application.service.StartSagaService;
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

    @Bean
    public StartSagaUseCase startSagaService(
            SagaRepository sagaRepository,
            SagaEventPublisher sagaEventPublisher) {
        return new StartSagaService(sagaRepository, sagaEventPublisher);
    }

    @Bean
    public AdvanceSagaUseCase advanceSagaService(
            SagaRepository sagaRepository,
            SagaEventPublisher sagaEventPublisher) {
        return new AdvanceSagaService(sagaRepository, sagaEventPublisher);
    }

    @Bean
    public CompensateSagaUseCase compensateSagaService(
            SagaRepository sagaRepository,
            SagaEventPublisher sagaEventPublisher) {
        return new CompensateSagaService(sagaRepository, sagaEventPublisher);
    }

    @Bean
    public HandleOrderCreatedUseCase handleOrderCreatedService(
            SagaRepository sagaRepository,
            SagaEventPublisher sagaEventPublisher,
            ReserveStockCommandPublisher reserveStockCommandPublisher) {
        return new HandleOrderCreatedService(sagaRepository, sagaEventPublisher,
                reserveStockCommandPublisher);
    }

    @Bean
    public HandleStockReservedUseCase handleStockReservedService(
            SagaRepository sagaRepository,
            SagaEventPublisher sagaEventPublisher,
            ReserveCreditCommandPublisher reserveCreditCommandPublisher) {
        return new HandleStockReservedService(sagaRepository, sagaEventPublisher,
                reserveCreditCommandPublisher);
    }

    @Bean
    public HandleCreditApprovedUseCase handleCreditApprovedService(
            SagaRepository sagaRepository,
            SagaEventPublisher sagaEventPublisher,
            ConfirmOrderCommandPublisher confirmOrderCommandPublisher) {
        return new HandleCreditApprovedService(sagaRepository, sagaEventPublisher,
                confirmOrderCommandPublisher);
    }
}
