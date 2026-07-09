package com.arbitrier.inventory.application.service;

import com.arbitrier.inventory.adapter.outbound.ConfigurableStockAvailabilityPort;
import com.arbitrier.inventory.adapter.outbound.InMemoryStockReservationRepository;
import com.arbitrier.inventory.adapter.outbound.RecordingStockReservationEventPublisher;
import com.arbitrier.inventory.application.port.inbound.ReserveStockCommand;
import com.arbitrier.inventory.application.port.inbound.ReserveStockLineCommand;
import com.arbitrier.inventory.application.port.inbound.ReserveStockResult;
import com.arbitrier.inventory.domain.model.StockReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ReserveStockService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class ReserveStockServiceTest {

    private static final String ORDER_ID = "order-001";
    private static final String RESERVATION_ID = "res-001";
    private static final String WAREHOUSE_ID = "wh-001";

    private ConfigurableStockAvailabilityPort availability;
    private InMemoryStockReservationRepository repository;
    private RecordingStockReservationEventPublisher publisher;
    private ReserveStockService service;

    @BeforeEach
    void setUp() {
        availability = new ConfigurableStockAvailabilityPort();
        repository = new InMemoryStockReservationRepository();
        publisher = new RecordingStockReservationEventPublisher();
        service = new ReserveStockService(availability, repository, publisher);
    }

    // ── Full reservation ──────────────────────────────────────────────────────

    @Test
    void full_stock_available_returns_reserved() {
        availability.setAvailable("SKU-A", 10);
        availability.setAvailable("SKU-B", 5);

        ReserveStockResult result = service.reserve(command(
                line("SKU-A", 10),
                line("SKU-B", 5)));

        assertThat(result.outcome()).isEqualTo(StockReservationStatus.RESERVED);
    }

    @Test
    void full_reservation_persists_to_repository() {
        availability.setAvailable("SKU-A", 10);

        service.reserve(command(line("SKU-A", 10)));

        assertThat(repository.size()).isEqualTo(1);
        var saved = repository.getById(
                com.arbitrier.inventory.domain.model.StockReservationId.of(RESERVATION_ID));
        assertThat(saved.status()).isEqualTo(StockReservationStatus.RESERVED);
    }

    @Test
    void full_reservation_publishes_stock_reserved_event() {
        availability.setAvailable("SKU-A", 10);

        service.reserve(command(line("SKU-A", 10)));

        assertThat(publisher.reservedEvents()).hasSize(1);
        assertThat(publisher.partiallyReservedEvents()).isEmpty();
        assertThat(publisher.rejectedEvents()).isEmpty();
    }

    @Test
    void full_reservation_event_carries_correct_ids() {
        availability.setAvailable("SKU-A", 10);

        service.reserve(command(line("SKU-A", 10)));

        var event = publisher.reservedEvents().get(0);
        assertThat(event.reservationId().value()).isEqualTo(RESERVATION_ID);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.warehouseId().value()).isEqualTo(WAREHOUSE_ID);
    }

    // ── Partial reservation ───────────────────────────────────────────────────

    @Test
    void partial_stock_returns_partially_reserved() {
        availability.setAvailable("SKU-A", 10);
        availability.setAvailable("SKU-B", 2); // only 2 of 5 requested

        ReserveStockResult result = service.reserve(command(
                line("SKU-A", 10),
                line("SKU-B", 5)));

        assertThat(result.outcome()).isEqualTo(StockReservationStatus.PARTIALLY_RESERVED);
    }

    @Test
    void partial_reservation_persists_partially_reserved_status() {
        availability.setAvailable("SKU-A", 10);
        availability.setAvailable("SKU-B", 0);

        service.reserve(command(line("SKU-A", 10), line("SKU-B", 5)));

        var saved = repository.getById(
                com.arbitrier.inventory.domain.model.StockReservationId.of(RESERVATION_ID));
        assertThat(saved.status()).isEqualTo(StockReservationStatus.PARTIALLY_RESERVED);
    }

    @Test
    void partial_reservation_publishes_partially_reserved_event() {
        availability.setAvailable("SKU-A", 3); // 3 of 10 requested

        service.reserve(command(line("SKU-A", 10)));

        assertThat(publisher.partiallyReservedEvents()).hasSize(1);
        assertThat(publisher.reservedEvents()).isEmpty();
        assertThat(publisher.rejectedEvents()).isEmpty();
    }

    // ── Rejection ─────────────────────────────────────────────────────────────

    @Test
    void no_stock_available_returns_rejected() {
        // SKU-A availability defaults to 0 (not configured)

        ReserveStockResult result = service.reserve(command(line("SKU-A", 10)));

        assertThat(result.outcome()).isEqualTo(StockReservationStatus.REJECTED);
    }

    @Test
    void rejected_reservation_persists_rejected_status() {
        service.reserve(command(line("SKU-A", 10)));

        var saved = repository.getById(
                com.arbitrier.inventory.domain.model.StockReservationId.of(RESERVATION_ID));
        assertThat(saved.status()).isEqualTo(StockReservationStatus.REJECTED);
    }

    @Test
    void rejected_reservation_publishes_rejected_event() {
        service.reserve(command(line("SKU-A", 10)));

        assertThat(publisher.rejectedEvents()).hasSize(1);
        assertThat(publisher.reservedEvents()).isEmpty();
        assertThat(publisher.partiallyReservedEvents()).isEmpty();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void missing_orderId_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ReserveStockCommand(
                        "", RESERVATION_ID, WAREHOUSE_ID, List.of(line("SKU-A", 1))))
                .withMessageContaining("orderId");
    }

    @Test
    void missing_reservationId_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ReserveStockCommand(
                        ORDER_ID, "", WAREHOUSE_ID, List.of(line("SKU-A", 1))))
                .withMessageContaining("reservationId");
    }

    @Test
    void missing_warehouseId_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ReserveStockCommand(
                        ORDER_ID, RESERVATION_ID, "", List.of(line("SKU-A", 1))))
                .withMessageContaining("warehouseId");
    }

    @Test
    void empty_lines_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ReserveStockCommand(
                        ORDER_ID, RESERVATION_ID, WAREHOUSE_ID, List.of()))
                .withMessageContaining("lines");
    }

    @Test
    void invalid_sku_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ReserveStockLineCommand("", 1))
                .withMessageContaining("sku");
    }

    @Test
    void zero_quantity_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ReserveStockLineCommand("SKU-A", 0))
                .withMessageContaining("quantity");
    }

    @Test
    void negative_quantity_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ReserveStockLineCommand("SKU-A", -1))
                .withMessageContaining("quantity");
    }

    @Test
    void result_carries_reservation_id() {
        availability.setAvailable("SKU-A", 10);

        ReserveStockResult result = service.reserve(command(line("SKU-A", 5)));

        assertThat(result.reservationId().value()).isEqualTo(RESERVATION_ID);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ReserveStockCommand command(ReserveStockLineCommand... lines) {
        return new ReserveStockCommand(ORDER_ID, RESERVATION_ID, WAREHOUSE_ID, List.of(lines));
    }

    private ReserveStockLineCommand line(String sku, int quantity) {
        return new ReserveStockLineCommand(sku, quantity);
    }
}
