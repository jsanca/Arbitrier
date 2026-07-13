package com.arbitrier.orchestrator.adapter.outbound.persistence;

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
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import com.arbitrier.orchestrator.domain.model.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository round-trip test for saga state against a Flyway-migrated schema.
 *
 * <p>Verifies that nullable fields (customerDecision, stockReservationId, creditReservationId)
 * round-trip correctly, and that a fully-populated saga state is reconstructed faithfully.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: orchestrator-service
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false"
        }
)
@Testcontainers
class RepositoryRoundTripIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("test-db/create-orchestrator-service-schema.sql");

    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        SagaEventPublisher sagaEventPublisher() {
            return new RecordingSagaEventPublisher();
        }

        @Bean
        @Primary
        ReserveStockCommandPublisher reserveStockCommandPublisher() {
            return new RecordingReserveStockCommandPublisher();
        }

        @Bean
        @Primary
        ReserveCreditCommandPublisher reserveCreditCommandPublisher() {
            return new RecordingReserveCreditCommandPublisher();
        }

        @Bean
        @Primary
        ConfirmOrderCommandPublisher confirmOrderCommandPublisher() {
            return new RecordingConfirmOrderCommandPublisher();
        }

        @Bean
        @Primary
        ReleaseStockCommandPublisher releaseStockCommandPublisher() {
            return new RecordingReleaseStockCommandPublisher();
        }

        @Bean
        @Primary
        ReleaseCreditCommandPublisher releaseCreditCommandPublisher() {
            return new RecordingReleaseCreditCommandPublisher();
        }
    }

    @Autowired
    SagaRepository sagaRepository;

    @Autowired
    SpringDataSagaRepository springDataSagaRepository;

    private static final SagaId SAGA_ID = SagaId.of("saga-rt-1");
    private static final String ORDER_ID = "order-rt-1";
    private static final String CUSTOMER_ID = "cust-rt-1";

    @BeforeEach
    void clean() {
        springDataSagaRepository.deleteById(SAGA_ID.value());
    }

    @Test
    void saga_round_trip_with_populated_reservation_ids() {
        Saga saga = Saga.reconstruct(SAGA_ID, ORDER_ID, CUSTOMER_ID,
                SagaStatus.AWAITING_CUSTOMER_DECISION, SagaStep.AWAIT_CUSTOMER_DECISION,
                null, "sr-rt-1", "cr-rt-1", null);
        sagaRepository.save(saga);

        Saga loaded = sagaRepository.findById(SAGA_ID).orElseThrow();

        assertThat(loaded.id()).isEqualTo(SAGA_ID);
        assertThat(loaded.orderId()).isEqualTo(ORDER_ID);
        assertThat(loaded.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(loaded.status()).isEqualTo(SagaStatus.AWAITING_CUSTOMER_DECISION);
        assertThat(loaded.currentStep()).isEqualTo(SagaStep.AWAIT_CUSTOMER_DECISION);
        assertThat(loaded.customerDecision()).isNull();
        assertThat(loaded.stockReservationId()).isEqualTo("sr-rt-1");
        assertThat(loaded.creditReservationId()).isEqualTo("cr-rt-1");
        assertThat(loaded.version()).isNotNull();
    }

    @Test
    void saga_nullable_fields_round_trip() {
        sagaRepository.save(Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID));

        Saga loaded = sagaRepository.findById(SAGA_ID).orElseThrow();

        assertThat(loaded.customerDecision()).isNull();
        assertThat(loaded.stockReservationId()).isNull();
        assertThat(loaded.creditReservationId()).isNull();
        assertThat(loaded.status()).isEqualTo(SagaStatus.STARTED);
        assertThat(loaded.version()).isNotNull();
    }
}
