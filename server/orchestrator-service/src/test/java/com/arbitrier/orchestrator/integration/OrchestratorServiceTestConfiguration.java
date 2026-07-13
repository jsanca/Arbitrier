package com.arbitrier.orchestrator.integration;

import com.arbitrier.orchestrator.adapter.outbound.InMemorySagaRepository;
import com.arbitrier.orchestrator.adapter.outbound.RecordingConfirmOrderCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReleaseCreditCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReserveCreditCommandPublisher;
import com.arbitrier.orchestrator.adapter.outbound.RecordingReserveStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ConfirmOrderCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseCreditCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveCreditCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.platform.messaging.inbox.InboxRepository;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.test.InMemoryInboxRepository;
import com.arbitrier.platform.messaging.test.InMemoryOutboxRepository;
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
    public OutboxRepository outboxRepository() {
        return new InMemoryOutboxRepository();
    }

    @Bean
    @Primary
    public InboxRepository inboxRepository() {
        return new InMemoryInboxRepository();
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
