package com.arbitrier.credit.domain;

import com.arbitrier.credit.domain.model.CreditReservation;
import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.credit.domain.model.CreditReservationStatus;
import com.arbitrier.credit.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link CreditReservation} aggregate root.
 */
class CreditReservationTest {

    private static final CreditReservationId RES_ID = CreditReservationId.of("cr-001");
    private static final String ORDER_ID = "order-001";
    private static final Money AMOUNT = Money.of(new BigDecimal("1000.00"), "USD");

    @Test
    void approved_creates_reservation_with_approved_status_and_correct_fields() {
        CreditReservation reservation = CreditReservation.approved(RES_ID, ORDER_ID, AMOUNT);

        assertThat(reservation.status()).isEqualTo(CreditReservationStatus.APPROVED);
        assertThat(reservation.id()).isEqualTo(RES_ID);
        assertThat(reservation.orderId()).isEqualTo(ORDER_ID);
        assertThat(reservation.amount()).isEqualTo(AMOUNT);
    }

    @Test
    void rejected_creates_reservation_with_rejected_status() {
        CreditReservation reservation = CreditReservation.rejected(RES_ID, ORDER_ID, AMOUNT);

        assertThat(reservation.status()).isEqualTo(CreditReservationStatus.REJECTED);
    }

    @Test
    void release_from_approved_transitions_to_released() {
        CreditReservation reservation = CreditReservation.approved(RES_ID, ORDER_ID, AMOUNT);

        CreditReservation released = reservation.release();

        assertThat(released.status()).isEqualTo(CreditReservationStatus.RELEASED);
    }

    @Test
    void release_is_idempotent_when_already_released() {
        CreditReservation reservation = CreditReservation.approved(RES_ID, ORDER_ID, AMOUNT);

        CreditReservation firstRelease = reservation.release();
        CreditReservation secondRelease = firstRelease.release();

        assertThat(secondRelease.status()).isEqualTo(CreditReservationStatus.RELEASED);
        // Second call returns same instance (idempotent)
        assertThat(secondRelease).isSameAs(firstRelease);
    }

    @Test
    void release_from_rejected_throws() {
        CreditReservation rejected = CreditReservation.rejected(RES_ID, ORDER_ID, AMOUNT);

        assertThatIllegalArgumentException()
                .isThrownBy(rejected::release)
                .withMessageContaining("APPROVED");
    }
}
