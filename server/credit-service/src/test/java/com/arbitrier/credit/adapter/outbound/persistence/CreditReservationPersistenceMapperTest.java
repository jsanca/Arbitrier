package com.arbitrier.credit.adapter.outbound.persistence;

import com.arbitrier.credit.domain.model.CreditReservation;
import com.arbitrier.credit.domain.model.CreditReservationId;
import com.arbitrier.credit.domain.model.CreditReservationStatus;
import com.arbitrier.credit.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CreditReservationPersistenceMapperTest {

    private static final CreditReservationPersistenceMapper MAPPER =
            new CreditReservationPersistenceMapper();

    private static final CreditReservationId RES_ID = CreditReservationId.of("cr-1");
    private static final String ORDER_ID = "order-1";
    private static final Money AMOUNT = Money.of(new BigDecimal("1500.00"), "USD");

    // ── toEntity / new reservation ────────────────────────────────────────────

    @Test
    void new_reservation_maps_to_entity_with_null_version() {
        CreditReservation reservation = CreditReservation.approved(RES_ID, ORDER_ID, AMOUNT);

        CreditReservationEntity entity = MAPPER.toEntity(reservation);

        assertThat(entity.getId()).isEqualTo("cr-1");
        assertThat(entity.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(entity.getAmountValue()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(entity.getAmountCurrency()).isEqualTo("USD");
        assertThat(entity.getStatus()).isEqualTo("APPROVED");
        assertThat(entity.getVersion()).isNull();
    }

    @Test
    void rejected_reservation_maps_status_correctly() {
        CreditReservation reservation = CreditReservation.rejected(RES_ID, ORDER_ID, AMOUNT);

        CreditReservationEntity entity = MAPPER.toEntity(reservation);

        assertThat(entity.getStatus()).isEqualTo("REJECTED");
    }

    // ── toDomain ─────────────────────────────────────────────────────────────

    @Test
    void entity_round_trips_to_domain_and_back() {
        CreditReservation original = CreditReservation.reconstruct(
                RES_ID, ORDER_ID, AMOUNT, CreditReservationStatus.APPROVED, 5L);

        CreditReservationEntity entity = MAPPER.toEntity(original);
        entity.setVersion(5L);
        CreditReservation restored = MAPPER.toDomain(entity);

        assertThat(restored.id()).isEqualTo(RES_ID);
        assertThat(restored.orderId()).isEqualTo(ORDER_ID);
        assertThat(restored.amount().amount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(restored.amount().currency()).isEqualTo("USD");
        assertThat(restored.status()).isEqualTo(CreditReservationStatus.APPROVED);
        assertThat(restored.version()).isEqualTo(5L);
    }

    @Test
    void toDomain_restores_released_status() {
        CreditReservation original = CreditReservation.reconstruct(
                RES_ID, ORDER_ID, AMOUNT, CreditReservationStatus.RELEASED, 3L);

        CreditReservation restored = MAPPER.toDomain(MAPPER.toEntity(original));

        assertThat(restored.status()).isEqualTo(CreditReservationStatus.RELEASED);
    }

    @Test
    void toDomain_restores_version() {
        CreditReservation original = CreditReservation.reconstruct(
                RES_ID, ORDER_ID, AMOUNT, CreditReservationStatus.APPROVED, 99L);

        CreditReservationEntity entity = MAPPER.toEntity(original);
        entity.setVersion(99L);

        assertThat(MAPPER.toDomain(entity).version()).isEqualTo(99L);
    }

    // ── updateEntity ─────────────────────────────────────────────────────────

    @Test
    void updateEntity_updates_status_and_preserves_entity_version() {
        CreditReservation initial = CreditReservation.approved(RES_ID, ORDER_ID, AMOUNT);
        CreditReservationEntity entity = MAPPER.toEntity(initial);
        entity.setVersion(2L);

        CreditReservation released = CreditReservation.reconstruct(
                RES_ID, ORDER_ID, AMOUNT, CreditReservationStatus.RELEASED, 2L);

        MAPPER.updateEntity(entity, released);

        assertThat(entity.getVersion()).isEqualTo(2L);
        assertThat(entity.getStatus()).isEqualTo("RELEASED");
        assertThat(entity.getAmountValue()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    // ── corrupt data ─────────────────────────────────────────────────────────

    @Test
    void toDomain_rejects_unknown_status() {
        CreditReservationEntity entity = buildEntity("UNKNOWN_STATUS");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> MAPPER.toDomain(entity))
                .withMessageContaining("UNKNOWN_STATUS");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static CreditReservationEntity buildEntity(String status) {
        CreditReservationEntity entity = new CreditReservationEntity();
        entity.setId("cr-1");
        entity.setOrderId("order-1");
        entity.setAmountValue(new BigDecimal("1000.00"));
        entity.setAmountCurrency("USD");
        entity.setStatus(status);
        entity.setVersion(1L);
        return entity;
    }
}
