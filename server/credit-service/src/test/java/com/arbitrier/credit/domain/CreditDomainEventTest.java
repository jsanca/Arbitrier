package com.arbitrier.credit.domain;

import com.arbitrier.credit.domain.event.CreditApprovedDomainEvent;
import com.arbitrier.credit.domain.event.CreditRejectedDomainEvent;
import com.arbitrier.credit.domain.event.CreditReleasedDomainEvent;
import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.credit.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for Credit domain event constructor validation.
 */
class CreditDomainEventTest {

    private static final CreditReservationId RES_ID = CreditReservationId.of("cr-001");
    private static final String ORDER_ID = "order-001";
    private static final String CUSTOMER_ID = "cust-001";
    private static final Money AMOUNT = Money.of(new BigDecimal("500.00"), "USD");

    // ── CreditApprovedDomainEvent ─────────────────────────────────────────────

    @Test
    void credit_approved_event_stores_all_fields() {
        CreditApprovedDomainEvent event = new CreditApprovedDomainEvent(RES_ID, ORDER_ID, CUSTOMER_ID, AMOUNT);

        assertThat(event.reservationId()).isEqualTo(RES_ID);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.amount()).isEqualTo(AMOUNT);
    }

    @Test
    void credit_approved_event_null_reservation_id_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CreditApprovedDomainEvent(null, ORDER_ID, CUSTOMER_ID, AMOUNT))
                .withMessageContaining("reservationId");
    }

    @Test
    void credit_approved_event_blank_order_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreditApprovedDomainEvent(RES_ID, "  ", CUSTOMER_ID, AMOUNT))
                .withMessageContaining("orderId");
    }

    @Test
    void credit_approved_event_blank_customer_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreditApprovedDomainEvent(RES_ID, ORDER_ID, "", AMOUNT))
                .withMessageContaining("customerId");
    }

    @Test
    void credit_approved_event_null_amount_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CreditApprovedDomainEvent(RES_ID, ORDER_ID, CUSTOMER_ID, null))
                .withMessageContaining("amount");
    }

    // ── CreditRejectedDomainEvent ─────────────────────────────────────────────

    @Test
    void credit_rejected_event_stores_all_fields() {
        CreditRejectedDomainEvent event = new CreditRejectedDomainEvent(RES_ID, ORDER_ID, CUSTOMER_ID, AMOUNT);

        assertThat(event.reservationId()).isEqualTo(RES_ID);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.amount()).isEqualTo(AMOUNT);
    }

    @Test
    void credit_rejected_event_null_reservation_id_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CreditRejectedDomainEvent(null, ORDER_ID, CUSTOMER_ID, AMOUNT))
                .withMessageContaining("reservationId");
    }

    @Test
    void credit_rejected_event_blank_order_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreditRejectedDomainEvent(RES_ID, "  ", CUSTOMER_ID, AMOUNT))
                .withMessageContaining("orderId");
    }

    @Test
    void credit_rejected_event_blank_customer_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreditRejectedDomainEvent(RES_ID, ORDER_ID, "  ", AMOUNT))
                .withMessageContaining("customerId");
    }

    @Test
    void credit_rejected_event_null_amount_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CreditRejectedDomainEvent(RES_ID, ORDER_ID, CUSTOMER_ID, null))
                .withMessageContaining("amount");
    }

    // ── CreditReleasedDomainEvent ─────────────────────────────────────────────

    @Test
    void credit_released_event_stores_all_fields() {
        CreditReleasedDomainEvent event = new CreditReleasedDomainEvent(RES_ID, ORDER_ID);

        assertThat(event.reservationId()).isEqualTo(RES_ID);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    void credit_released_event_null_reservation_id_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CreditReleasedDomainEvent(null, ORDER_ID))
                .withMessageContaining("reservationId");
    }

    @Test
    void credit_released_event_blank_order_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CreditReleasedDomainEvent(RES_ID, ""))
                .withMessageContaining("orderId");
    }
}
