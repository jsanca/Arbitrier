package com.arbitrier.orchestrator.integration;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingConfirmOrderCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReleaseCreditCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReserveCreditCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReserveStockCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingSagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ConfirmOrderCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseCreditCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveCreditCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration: wires in-memory adapters for Spring context load tests.
 *
 * <p>Layer: integration test
 * <p>Module: orchestrator-service
 */
@TestConfiguration
public class OrchestratorServiceTestConfiguration {

    @Bean
    @Primary
    public SagaRepository sagaRepository() {
        return new InMemorySagaRepository();
    }

    @Bean
    @Primary
    public SagaEventPublisher sagaEventPublisher() {
        return new RecordingSagaEventPublisher();
    }

    @Bean
    @Primary
    public ReserveStockCommandPublisher reserveStockCommandPublisher() {
        return new RecordingReserveStockCommandPublisher();
    }

    @Bean
    @Primary
    public ReserveCreditCommandPublisher reserveCreditCommandPublisher() {
        return new RecordingReserveCreditCommandPublisher();
    }

    @Bean
    @Primary
    public ConfirmOrderCommandPublisher confirmOrderCommandPublisher() {
        return new RecordingConfirmOrderCommandPublisher();
    }

    @Bean
    @Primary
    public ReleaseStockCommandPublisher releaseStockCommandPublisher() {
        return new RecordingReleaseStockCommandPublisher();
    }

    @Bean
    @Primary
    public ReleaseCreditCommandPublisher releaseCreditCommandPublisher() {
        return new RecordingReleaseCreditCommandPublisher();
    }
}
