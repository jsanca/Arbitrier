package com.arbitrier.credit.adapter.outbound.persistence;

import com.arbitrier.credit.adapter.outbound.ConfigurableCreditLimitPort;
import com.arbitrier.credit.adapter.outbound.RecordingCreditReservationEventPublisher;
import com.arbitrier.credit.application.port.outbound.CreditLimitPort;
import com.arbitrier.credit.application.port.outbound.CreditReservationEventPublisher;
import com.arbitrier.credit.application.port.outbound.CreditReservationRepository;
import com.arbitrier.credit.domain.model.CreditReservation;
import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.credit.domain.model.CreditReservationStatus;
import com.arbitrier.credit.domain.model.Money;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the JPA credit-reservation persistence adapter using Testcontainers.
 *
 * <p>Verifies save/load round-trip, status update, amount fields, and optimistic-lock conflict.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false"
        }
)
@Testcontainers
class JpaCreditReservationRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("test-db/create-credit-service-schema.sql");

    /** Stubs for non-persistence ports — CreditReservationRepository is provided by JPA config. */
    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        CreditLimitPort creditLimitPort() {
            return new ConfigurableCreditLimitPort();
        }

        @Bean
        @Primary
        CreditReservationEventPublisher creditReservationEventPublisher() {
            return new RecordingCreditReservationEventPublisher();
        }
    }

    @Autowired
    CreditReservationRepository creditReservationRepository;

    private static final CreditReservationId RES_ID = CreditReservationId.of("cr-tc-1");
    private static final String ORDER_ID = "order-1";
    private static final Money AMOUNT = Money.of(new BigDecimal("2500.00"), "EUR");

    // ── save and load ─────────────────────────────────────────────────────────

    @Test
    void save_and_load_new_approved_reservation() {
        creditReservationRepository.save(CreditReservation.approved(RES_ID, ORDER_ID, AMOUNT));

        Optional<CreditReservation> loaded = creditReservationRepository.findById(RES_ID);

        assertThat(loaded).isPresent();
        CreditReservation result = loaded.get();
        assertThat(result.id()).isEqualTo(RES_ID);
        assertThat(result.orderId()).isEqualTo(ORDER_ID);
        assertThat(result.status()).isEqualTo(CreditReservationStatus.APPROVED);
        assertThat(result.amount().amount()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(result.amount().currency()).isEqualTo("EUR");
        assertThat(result.version()).isNotNull();
    }

    @Test
    void save_and_load_rejected_reservation() {
        creditReservationRepository.save(CreditReservation.rejected(RES_ID, ORDER_ID, AMOUNT));

        CreditReservation loaded = creditReservationRepository.findById(RES_ID).orElseThrow();
        assertThat(loaded.status()).isEqualTo(CreditReservationStatus.REJECTED);
    }

    // ── status update ─────────────────────────────────────────────────────────

    @Test
    void save_updates_reservation_status_to_released() {
        creditReservationRepository.save(CreditReservation.approved(RES_ID, ORDER_ID, AMOUNT));

        CreditReservation loaded = creditReservationRepository.findById(RES_ID).orElseThrow();
        creditReservationRepository.save(loaded.release());

        CreditReservation updated = creditReservationRepository.findById(RES_ID).orElseThrow();
        assertThat(updated.status()).isEqualTo(CreditReservationStatus.RELEASED);
    }

    // ── optimistic lock conflict ──────────────────────────────────────────────

    @Test
    void optimistic_lock_conflict_throws_typed_exception() {
        creditReservationRepository.save(CreditReservation.approved(RES_ID, ORDER_ID, AMOUNT));

        CreditReservation staleLoad = creditReservationRepository.findById(RES_ID).orElseThrow();
        // Advance DB version by saving once with the current version
        creditReservationRepository.save(staleLoad.release());

        // staleLoad still carries version 0; DB is now at version 1
        assertThatThrownBy(() -> creditReservationRepository.save(staleLoad.release()))
                .isInstanceOf(ApplicationProblemException.class)
                .satisfies(ex -> assertThat(((ApplicationProblemException) ex).code())
                        .isEqualTo(PersistenceProblemCode.OPTIMISTIC_LOCK_CONFLICT));
    }

    // ── findById not found ────────────────────────────────────────────────────

    @Test
    void findById_returns_empty_for_unknown_id() {
        Optional<CreditReservation> result =
                creditReservationRepository.findById(CreditReservationId.of("not-existing"));

        assertThat(result).isEmpty();
    }
}
