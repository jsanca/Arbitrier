package com.arbitrier.inventory.application.service;

import com.arbitrier.inventory.adapter.outbound.ConfigurableStockAvailabilityPort;
import com.arbitrier.inventory.adapter.outbound.InMemoryStockReservationRepository;
import com.arbitrier.inventory.adapter.outbound.RecordingStockReservationEventPublisher;
import com.arbitrier.inventory.application.port.inbound.ReleaseStockCommand;
import com.arbitrier.inventory.application.port.inbound.ReleaseStockResult;
import com.arbitrier.inventory.application.port.inbound.ReserveStockCommand;
import com.arbitrier.inventory.application.port.inbound.ReserveStockLineCommand;
import com.arbitrier.inventory.domain.model.StockReservation;
import com.arbitrier.inventory.domain.model.StockReservationId;
import com.arbitrier.inventory.domain.model.StockReservationLine;
import com.arbitrier.inventory.domain.model.StockReservationStatus;
import com.arbitrier.inventory.domain.model.WarehouseId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ReleaseStockService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class ReleaseStockServiceTest {

    private static final String ORDER_ID = "order-release-001";
    private static final String RESERVATION_ID = "res-release-001";
    private static final String WAREHOUSE_ID = "wh-001";

    private static final StockReservationId RES_ID = StockReservationId.of(RESERVATION_ID);
    private static final WarehouseId WH_ID = WarehouseId.of(WAREHOUSE_ID);

    private InMemoryStockReservationRepository repository;
    private RecordingStockReservationEventPublisher publisher;
    private ReleaseStockService releaseService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryStockReservationRepository();
        publisher = new RecordingStockReservationEventPublisher();
        releaseService = new ReleaseStockService(repository, publisher);
    }

    // ── Release RESERVED reservation ──────────────────────────────────────────

    @Test
    void release_reserved_reservation_returns_released_result() {
        saveReserved();

        ReleaseStockResult result = releaseService.release(new ReleaseStockCommand(RESERVATION_ID));

        assertThat(result.reservationId()).isEqualTo(RES_ID);
    }

    @Test
    void release_reserved_reservation_persists_released_status() {
        saveReserved();

        releaseService.release(new ReleaseStockCommand(RESERVATION_ID));

        assertThat(repository.getById(RES_ID).status()).isEqualTo(StockReservationStatus.RELEASED);
    }

    @Test
    void release_reserved_reservation_publishes_released_event() {
        saveReserved();

        releaseService.release(new ReleaseStockCommand(RESERVATION_ID));

        assertThat(publisher.releasedEvents()).hasSize(1);
        assertThat(publisher.releasedEvents().get(0).reservationId()).isEqualTo(RES_ID);
        assertThat(publisher.releasedEvents().get(0).orderId()).isEqualTo(ORDER_ID);
    }

    // ── Release PARTIALLY_RESERVED reservation ────────────────────────────────

    @Test
    void release_partially_reserved_reservation_persists_released_status() {
        savePartiallyReserved();

        releaseService.release(new ReleaseStockCommand(RESERVATION_ID));

        assertThat(repository.getById(RES_ID).status()).isEqualTo(StockReservationStatus.RELEASED);
    }

    @Test
    void release_partially_reserved_reservation_publishes_released_event() {
        savePartiallyReserved();

        releaseService.release(new ReleaseStockCommand(RESERVATION_ID));

        assertThat(publisher.releasedEvents()).hasSize(1);
    }

    // ── Idempotency ───────────────────────────────────────────────────────────

    @Test
    void release_already_released_is_idempotent_returns_same_result() {
        saveReserved();
        releaseService.release(new ReleaseStockCommand(RESERVATION_ID)); // first call

        ReleaseStockResult secondResult = releaseService.release(new ReleaseStockCommand(RESERVATION_ID));

        assertThat(secondResult.reservationId()).isEqualTo(RES_ID);
    }

    @Test
    void release_already_released_does_not_publish_duplicate_event() {
        saveReserved();
        releaseService.release(new ReleaseStockCommand(RESERVATION_ID)); // first: publishes
        publisher.releasedEvents(); // confirm first event

        releaseService.release(new ReleaseStockCommand(RESERVATION_ID)); // second: no-op

        assertThat(publisher.releasedEvents()).hasSize(1); // only one event total
    }

    // ── Release REJECTED reservation ─────────────────────────────────────────

    @Test
    void release_rejected_reservation_is_no_op_returns_result() {
        // REJECTED reservations held no stock; release is a no-op (no persist, no event).
        // Decision documented in ReleaseStockService Javadoc.
        saveRejected();

        ReleaseStockResult result = releaseService.release(new ReleaseStockCommand(RESERVATION_ID));

        assertThat(result.reservationId()).isEqualTo(RES_ID);
    }

    @Test
    void release_rejected_reservation_does_not_publish_event() {
        saveRejected();

        releaseService.release(new ReleaseStockCommand(RESERVATION_ID));

        assertThat(publisher.totalEventCount()).isZero();
    }

    @Test
    void release_rejected_reservation_does_not_change_persisted_status() {
        saveRejected();

        releaseService.release(new ReleaseStockCommand(RESERVATION_ID));

        // Status remains REJECTED — no state transition for a no-op release
        assertThat(repository.getById(RES_ID).status()).isEqualTo(StockReservationStatus.REJECTED);
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    void release_non_existent_reservation_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> releaseService.release(new ReleaseStockCommand("does-not-exist")))
                .withMessageContaining("does-not-exist");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_reservation_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ReleaseStockCommand(""))
                .withMessageContaining("reservationId");
    }

    // ── Repository interactions ───────────────────────────────────────────────

    @Test
    void release_calls_repository_save_for_reserved_reservation() {
        saveReserved();

        releaseService.release(new ReleaseStockCommand(RESERVATION_ID));

        // Verify the saved reservation has RELEASED status (i.e., save was called with new state)
        assertThat(repository.getById(RES_ID).status()).isEqualTo(StockReservationStatus.RELEASED);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void saveReserved() {
        StockReservation r = StockReservation.fullyReserved(
                RES_ID, ORDER_ID, WH_ID,
                List.of(new StockReservationLine("SKU-A", 10, 10)));
        repository.save(r);
    }

    private void savePartiallyReserved() {
        StockReservation r = StockReservation.partiallyReserved(
                RES_ID, ORDER_ID, WH_ID,
                List.of(
                        new StockReservationLine("SKU-A", 10, 10),
                        new StockReservationLine("SKU-B", 5, 0)));
        repository.save(r);
    }

    private void saveRejected() {
        StockReservation r = StockReservation.rejected(
                RES_ID, ORDER_ID, WH_ID,
                List.of(new StockReservationLine("SKU-A", 10, 0)));
        repository.save(r);
    }
}
