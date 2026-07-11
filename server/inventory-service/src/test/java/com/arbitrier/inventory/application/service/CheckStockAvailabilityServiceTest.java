package com.arbitrier.inventory.application.service;

import com.arbitrier.inventory.adapter.outbound.ConfigurableWarehouseAllocationPort;
import com.arbitrier.inventory.application.port.inbound.CheckStockAvailabilityCommand;
import com.arbitrier.inventory.application.port.inbound.CheckStockAvailabilityLineCommand;
import com.arbitrier.inventory.application.port.inbound.CheckStockAvailabilityLineResult;
import com.arbitrier.inventory.application.port.inbound.CheckStockAvailabilityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link CheckStockAvailabilityService}.
 *
 * <p>No Spring context or external dependencies required.
 */
class CheckStockAvailabilityServiceTest {

    private static final String SKU_A = "SKU-A";
    private static final String SKU_B = "SKU-B";

    private ConfigurableWarehouseAllocationPort stockPort;
    private CheckStockAvailabilityService service;

    @BeforeEach
    void setUp() {
        stockPort = new ConfigurableWarehouseAllocationPort();
        service = new CheckStockAvailabilityService(stockPort);
    }

    // ── Fully available ───────────────────────────────────────────────────────

    @Test
    void all_lines_fully_available_returns_full_results() {
        stockPort.setAvailable(SKU_A, 100);
        stockPort.setAvailable(SKU_B, 50);

        CheckStockAvailabilityResult result = service.check(command(
                line(SKU_A, 10),
                line(SKU_B, 20)));

        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines()).allMatch(CheckStockAvailabilityLineResult::fullyAvailable);
        assertThat(result.lines()).allMatch(l -> l.backorderQuantity() == 0);
    }

    @Test
    void available_quantity_capped_at_requested_when_stock_exceeds_request() {
        stockPort.setAvailable(SKU_A, 999);

        CheckStockAvailabilityResult result = service.check(command(line(SKU_A, 5)));

        CheckStockAvailabilityLineResult line = result.lines().get(0);
        assertThat(line.availableQuantity()).isEqualTo(5);
        assertThat(line.backorderQuantity()).isEqualTo(0);
        assertThat(line.fullyAvailable()).isTrue();
    }

    // ── Partial availability ──────────────────────────────────────────────────

    @Test
    void partially_available_line_returns_correct_quantities() {
        stockPort.setAvailable(SKU_A, 3);

        CheckStockAvailabilityResult result = service.check(command(line(SKU_A, 10)));

        CheckStockAvailabilityLineResult line = result.lines().get(0);
        assertThat(line.availableQuantity()).isEqualTo(3);
        assertThat(line.backorderQuantity()).isEqualTo(7);
        assertThat(line.fullyAvailable()).isFalse();
    }

    @Test
    void backorder_quantity_equals_requested_minus_available() {
        stockPort.setAvailable(SKU_A, 4);

        CheckStockAvailabilityResult result = service.check(command(line(SKU_A, 9)));

        CheckStockAvailabilityLineResult line = result.lines().get(0);
        assertThat(line.availableQuantity()).isEqualTo(4);
        assertThat(line.backorderQuantity()).isEqualTo(5);
    }

    // ── No stock ──────────────────────────────────────────────────────────────

    @Test
    void no_stock_available_returns_zero_available_for_all_lines() {
        // no availability set — ConfigurableWarehouseAllocationPort returns 0 by default

        CheckStockAvailabilityResult result = service.check(command(
                line(SKU_A, 10),
                line(SKU_B, 5)));

        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines()).allMatch(l -> l.availableQuantity() == 0);
        assertThat(result.lines()).noneMatch(CheckStockAvailabilityLineResult::fullyAvailable);
        assertThat(result.lines()).allMatch(l -> l.backorderQuantity() == l.requestedQuantity());
    }

    @Test
    void zero_available_results_in_full_backorder_quantity() {
        stockPort.setAvailable(SKU_A, 0);

        CheckStockAvailabilityResult result = service.check(command(line(SKU_A, 7)));

        CheckStockAvailabilityLineResult line = result.lines().get(0);
        assertThat(line.availableQuantity()).isEqualTo(0);
        assertThat(line.backorderQuantity()).isEqualTo(7);
        assertThat(line.fullyAvailable()).isFalse();
    }

    // ── Multi-warehouse ───────────────────────────────────────────────────────

    @Test
    void global_stock_spanning_multiple_warehouses_is_summed() {
        stockPort.setAvailable("wh-001", SKU_A, 3);
        stockPort.setAvailable("wh-002", SKU_A, 4);

        CheckStockAvailabilityResult result = service.check(command(line(SKU_A, 7)));

        CheckStockAvailabilityLineResult line = result.lines().get(0);
        assertThat(line.availableQuantity()).isEqualTo(7);
        assertThat(line.fullyAvailable()).isTrue();
        assertThat(line.backorderQuantity()).isEqualTo(0);
    }

    // ── Mixed lines ───────────────────────────────────────────────────────────

    @Test
    void mixed_availability_returns_correct_per_line_results() {
        stockPort.setAvailable(SKU_A, 100); // fully available
        stockPort.setAvailable(SKU_B, 2);   // partially available

        CheckStockAvailabilityResult result = service.check(command(
                line(SKU_A, 10),
                line(SKU_B, 5)));

        CheckStockAvailabilityLineResult lineA = findBySku(result, SKU_A);
        CheckStockAvailabilityLineResult lineB = findBySku(result, SKU_B);

        assertThat(lineA.fullyAvailable()).isTrue();
        assertThat(lineA.backorderQuantity()).isEqualTo(0);

        assertThat(lineB.availableQuantity()).isEqualTo(2);
        assertThat(lineB.backorderQuantity()).isEqualTo(3);
        assertThat(lineB.fullyAvailable()).isFalse();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void empty_lines_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.check(new CheckStockAvailabilityCommand(List.of())))
                .withMessageContaining("lines");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CheckStockAvailabilityCommand command(CheckStockAvailabilityLineCommand... lines) {
        return new CheckStockAvailabilityCommand(List.of(lines));
    }

    private CheckStockAvailabilityLineCommand line(String sku, int quantity) {
        return new CheckStockAvailabilityLineCommand(sku, quantity);
    }

    private CheckStockAvailabilityLineResult findBySku(CheckStockAvailabilityResult result, String sku) {
        return result.lines().stream()
                .filter(l -> l.sku().equals(sku))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No result for SKU: " + sku));
    }
}
