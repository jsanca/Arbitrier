package com.arbitrier.inventory.domain;

import com.arbitrier.inventory.domain.model.StockReservation;
import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.StockReservationLine;
import com.arbitrier.inventory.domain.model.StockReservationStatus;
import com.arbitrier.inventory.domain.model.WarehouseId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link StockReservation} aggregate root and related value objects.
 */
class StockReservationTest {

    private static final StockReservationId RES_ID = StockReservationId.of("res-001");
    private static final String ORDER_ID = "order-001";
    private static final WarehouseId WAREHOUSE_ID = WarehouseId.of("wh-001");

    private static StockReservationLine fullyReservedLine() {
        return new StockReservationLine("SKU-A", 10, 10);
    }

    private static StockReservationLine partialLine() {
        return new StockReservationLine("SKU-B", 10, 5);
    }

    private static StockReservationLine unreservedLine() {
        return new StockReservationLine("SKU-C", 10, 0);
    }

    @Test
    void fully_reserved_creates_reservation_with_reserved_status() {
        List<StockReservationLine> lines = List.of(fullyReservedLine());

        StockReservation reservation = StockReservation.fullyReserved(RES_ID, ORDER_ID, WAREHOUSE_ID, lines);

        assertThat(reservation.status()).isEqualTo(StockReservationStatus.RESERVED);
        assertThat(reservation.orderId()).isEqualTo(ORDER_ID);
        assertThat(reservation.lines()).hasSize(1);
    }

    @Test
    void fully_reserved_rejects_partially_reserved_lines() {
        List<StockReservationLine> lines = List.of(fullyReservedLine(), partialLine());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> StockReservation.fullyReserved(RES_ID, ORDER_ID, WAREHOUSE_ID, lines))
                .withMessageContaining("fully reserved");
    }

    @Test
    void partially_reserved_creates_reservation_with_partially_reserved_status() {
        List<StockReservationLine> lines = List.of(fullyReservedLine(), unreservedLine());

        StockReservation reservation = StockReservation.partiallyReserved(RES_ID, ORDER_ID, WAREHOUSE_ID, lines);

        assertThat(reservation.status()).isEqualTo(StockReservationStatus.PARTIALLY_RESERVED);
    }

    @Test
    void partially_reserved_rejects_when_all_lines_are_fully_reserved() {
        List<StockReservationLine> lines = List.of(fullyReservedLine(),
                new StockReservationLine("SKU-D", 3, 3));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> StockReservation.partiallyReserved(RES_ID, ORDER_ID, WAREHOUSE_ID, lines))
                .withMessageContaining("all lines are fully reserved");
    }

    @Test
    void release_transitions_to_released_status() {
        StockReservation reservation = StockReservation.fullyReserved(
                RES_ID, ORDER_ID, WAREHOUSE_ID, List.of(fullyReservedLine()));

        StockReservation released = reservation.release();

        assertThat(released.status()).isEqualTo(StockReservationStatus.RELEASED);
    }

    @Test
    void release_is_idempotent_when_already_released() {
        StockReservation reservation = StockReservation.fullyReserved(
                RES_ID, ORDER_ID, WAREHOUSE_ID, List.of(fullyReservedLine()));

        StockReservation firstRelease = reservation.release();
        StockReservation secondRelease = firstRelease.release();

        assertThat(secondRelease.status()).isEqualTo(StockReservationStatus.RELEASED);
        // Second call returns same instance (idempotent)
        assertThat(secondRelease).isSameAs(firstRelease);
    }

    @Test
    void stock_reservation_line_rejects_zero_requested_quantity() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new StockReservationLine("SKU-A", 0, 0))
                .withMessageContaining("requestedQuantity must be positive");
    }

    @Test
    void stock_reservation_line_rejects_negative_requested_quantity() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new StockReservationLine("SKU-A", -1, 0))
                .withMessageContaining("requestedQuantity must be positive");
    }

    @Test
    void stock_reservation_line_rejects_reserved_exceeding_requested() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new StockReservationLine("SKU-A", 5, 6))
                .withMessageContaining("reservedQuantity must not exceed requestedQuantity");
    }
}
