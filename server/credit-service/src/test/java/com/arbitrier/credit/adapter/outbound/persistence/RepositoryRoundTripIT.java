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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository round-trip test for credit reservations against a Flyway-migrated schema.
 *
 * <p>Verifies Money amount/currency roundtrip through NUMERIC(19,4) column.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: credit-service
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
            .withInitScript("test-db/create-credit-service-schema.sql");

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

    @Autowired
    SpringDataCreditReservationRepository springDataCreditReservationRepository;

    private static final CreditReservationId RES_ID = CreditReservationId.of("cr-rt-1");
    private static final String ORDER_ID = "order-rt-1";
    private static final Money AMOUNT = Money.of(new BigDecimal("2500.00"), "EUR");

    @BeforeEach
    void clean() {
        springDataCreditReservationRepository.deleteById(RES_ID.value());
    }

    @Test
    void credit_reservation_round_trip_through_flyway_migrated_schema() {
        creditReservationRepository.save(CreditReservation.approved(RES_ID, ORDER_ID, AMOUNT));

        CreditReservation loaded = creditReservationRepository.findById(RES_ID).orElseThrow();

        assertThat(loaded.id()).isEqualTo(RES_ID);
        assertThat(loaded.orderId()).isEqualTo(ORDER_ID);
        assertThat(loaded.status()).isEqualTo(CreditReservationStatus.APPROVED);
        assertThat(loaded.amount().amount()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(loaded.amount().currency()).isEqualTo("EUR");
        assertThat(loaded.version()).isNotNull();
    }

    @Test
    void rejected_reservation_round_trip() {
        creditReservationRepository.save(CreditReservation.rejected(RES_ID, ORDER_ID, AMOUNT));

        CreditReservation loaded = creditReservationRepository.findById(RES_ID).orElseThrow();

        assertThat(loaded.status()).isEqualTo(CreditReservationStatus.REJECTED);
    }
}
