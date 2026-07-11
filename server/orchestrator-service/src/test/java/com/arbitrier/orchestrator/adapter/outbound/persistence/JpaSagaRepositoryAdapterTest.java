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
import com.arbitrier.orchestrator.domain.model.CustomerDecision;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import com.arbitrier.orchestrator.domain.model.SagaStep;
import com.arbitrier.platform.error.ApplicationProblemException;
import com.arbitrier.platform.error.PersistenceProblemCode;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the JPA saga persistence adapter using Testcontainers PostgreSQL.
 *
 * <p>Verifies save/load of complete saga state, semantic transitions, nullable fields,
 * and optimistic-lock conflict detection.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false"
        }
)
@Testcontainers
class JpaSagaRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("test-db/create-orchestrator-service-schema.sql");

    /** Stubs for non-persistence ports — SagaRepository is provided by JPA config. */
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

    private static final SagaId SAGA_ID = SagaId.of("saga-tc-1");
    private static final String ORDER_ID = "order-1";
    private static final String CUSTOMER_ID = "cust-1";

    // ── save and load ─────────────────────────────────────────────────────────

    @Test
    void save_and_load_new_saga_with_null_optional_fields() {
        sagaRepository.save(Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID));

        Optional<Saga> loaded = sagaRepository.findById(SAGA_ID);

        assertThat(loaded).isPresent();
        Saga result = loaded.get();
        assertThat(result.id()).isEqualTo(SAGA_ID);
        assertThat(result.orderId()).isEqualTo(ORDER_ID);
        assertThat(result.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(result.status()).isEqualTo(SagaStatus.STARTED);
        assertThat(result.currentStep()).isEqualTo(SagaStep.ORDER_CREATED);
        assertThat(result.customerDecision()).isNull();
        assertThat(result.stockReservationId()).isNull();
        assertThat(result.creditReservationId()).isNull();
        assertThat(result.version()).isNotNull();
    }

    @Test
    void save_and_load_completed_saga_with_all_fields() {
        Saga saga = Saga.reconstruct(SAGA_ID, ORDER_ID, CUSTOMER_ID,
                SagaStatus.COMPLETED, SagaStep.COMPLETE_ORDER,
                CustomerDecision.ACCEPT_PARTIAL, "stock-res-1", "credit-res-1", null);
        sagaRepository.save(saga);

        Saga loaded = sagaRepository.findById(SAGA_ID).orElseThrow();
        assertThat(loaded.status()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(loaded.customerDecision()).isEqualTo(CustomerDecision.ACCEPT_PARTIAL);
        assertThat(loaded.stockReservationId()).isEqualTo("stock-res-1");
        assertThat(loaded.creditReservationId()).isEqualTo("credit-res-1");
    }

    // ── semantic transition updates ───────────────────────────────────────────

    @Test
    void save_updates_saga_after_inventory_reserved() {
        sagaRepository.save(Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID));

        Saga loaded = sagaRepository.findById(SAGA_ID).orElseThrow();
        sagaRepository.save(loaded.inventoryReserved("stock-res-1"));

        Saga updated = sagaRepository.findById(SAGA_ID).orElseThrow();
        assertThat(updated.currentStep()).isEqualTo(SagaStep.VALIDATE_CREDIT);
        assertThat(updated.stockReservationId()).isEqualTo("stock-res-1");
    }

    @Test
    void save_updates_saga_status_on_cancel() {
        sagaRepository.save(Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID));

        Saga loaded = sagaRepository.findById(SAGA_ID).orElseThrow();
        sagaRepository.save(loaded.cancel());

        Saga updated = sagaRepository.findById(SAGA_ID).orElseThrow();
        assertThat(updated.status()).isEqualTo(SagaStatus.CANCELLED);
    }

    @Test
    void save_persists_customer_decision() {
        sagaRepository.save(Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID));

        Saga loaded = sagaRepository.findById(SAGA_ID).orElseThrow();
        sagaRepository.save(loaded.awaitCustomerDecision());

        Saga waiting = sagaRepository.findById(SAGA_ID).orElseThrow();
        sagaRepository.save(waiting.applyCustomerDecision(CustomerDecision.CANCEL_ORDER));

        Saga withDecision = sagaRepository.findById(SAGA_ID).orElseThrow();
        assertThat(withDecision.customerDecision()).isEqualTo(CustomerDecision.CANCEL_ORDER);
    }

    // ── optimistic lock conflict ──────────────────────────────────────────────

    @Test
    void optimistic_lock_conflict_throws_typed_exception() {
        sagaRepository.save(Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID));

        Saga staleLoad = sagaRepository.findById(SAGA_ID).orElseThrow();
        // Advance DB version by saving once with the current version
        sagaRepository.save(staleLoad.cancel());

        // staleLoad still carries version 0; DB is now at version 1
        assertThatThrownBy(() -> sagaRepository.save(staleLoad.cancel()))
                .isInstanceOf(ApplicationProblemException.class)
                .satisfies(ex -> assertThat(((ApplicationProblemException) ex).code())
                        .isEqualTo(PersistenceProblemCode.OPTIMISTIC_LOCK_CONFLICT));
    }

    // ── findById not found ────────────────────────────────────────────────────

    @Test
    void findById_returns_empty_for_unknown_id() {
        Optional<Saga> result = sagaRepository.findById(SagaId.of("not-existing"));

        assertThat(result).isEmpty();
    }
}
