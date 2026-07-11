package com.arbitrier.order.application.service;

import com.arbitrier.order.adapter.outbound.StubInventoryAvailabilityPort;
import com.arbitrier.order.application.port.inbound.CustomerPreSagaDecision;
import com.arbitrier.order.application.port.inbound.PrepareCorporateBulkOrderCommand;
import com.arbitrier.order.application.port.inbound.PrepareCorporateBulkOrderLineCommand;
import com.arbitrier.order.application.port.inbound.PrepareCorporateBulkOrderLineResult;
import com.arbitrier.order.application.port.inbound.PrepareCorporateBulkOrderResult;
import com.arbitrier.order.application.port.inbound.RecommendedAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link PrepareCorporateBulkOrderService}.
 *
 * <p>No Spring context, Order aggregate, or external dependencies required.
 */
class PrepareCorporateBulkOrderServiceTest {

    private static final String CUSTOMER_ID = "cust-001";
    private static final String USER_ID = "user-001";
    private static final String SKU_A = "SKU-A";
    private static final String SKU_B = "SKU-B";
    private static final String SKU_C = "SKU-C";

    private StubInventoryAvailabilityPort inventoryPort;
    private PrepareCorporateBulkOrderService service;

    @BeforeEach
    void setUp() {
        inventoryPort = new StubInventoryAvailabilityPort();
        service = new PrepareCorporateBulkOrderService(inventoryPort);
    }

    // ── PROCEED_FULL ─────────────────────────────────────────────────────────

    @Test
    void prepare_returns_proceed_full_when_all_lines_fully_available() {
        inventoryPort.setUnlimited(SKU_A, SKU_B);

        PrepareCorporateBulkOrderResult result = service.prepare(command(
                line(SKU_A, 10), line(SKU_B, 5)));

        assertThat(result.recommendedAction()).isEqualTo(RecommendedAction.PROCEED_FULL);
        assertThat(result.allAvailable()).isTrue();
    }

    @Test
    void proceed_full_result_has_no_backorder_lines() {
        inventoryPort.setUnlimited(SKU_A, SKU_B);

        PrepareCorporateBulkOrderResult result = service.prepare(command(
                line(SKU_A, 10), line(SKU_B, 5)));

        assertThat(result.backorderLines()).isEmpty();
        assertThat(result.availableLines()).hasSize(2);
    }

    // ── ASK_CUSTOMER_ACCEPT_PARTIAL ──────────────────────────────────────────

    @Test
    void prepare_returns_ask_customer_when_partial_availability() {
        inventoryPort.setUnlimited(SKU_A);
        inventoryPort.setAvailable(SKU_B, 3); // requested 5, only 3 available

        PrepareCorporateBulkOrderResult result = service.prepare(command(
                line(SKU_A, 10), line(SKU_B, 5)));

        assertThat(result.recommendedAction()).isEqualTo(RecommendedAction.ASK_CUSTOMER_ACCEPT_PARTIAL);
        assertThat(result.allAvailable()).isFalse();
    }

    @Test
    void partial_result_available_lines_contain_available_quantities_only() {
        inventoryPort.setUnlimited(SKU_A);
        inventoryPort.setAvailable(SKU_B, 3);

        PrepareCorporateBulkOrderResult result = service.prepare(command(
                line(SKU_A, 10), line(SKU_B, 5)));

        // both SKUs appear in availableLines because both have some stock
        assertThat(result.availableLines()).hasSize(2);

        PrepareCorporateBulkOrderLineResult lineB = findBySku(result.availableLines(), SKU_B);
        assertThat(lineB.availableQuantity()).isEqualTo(3);   // only available qty
        assertThat(lineB.requestedQuantity()).isEqualTo(5);   // original request preserved
        assertThat(lineB.backorderQuantity()).isEqualTo(2);
    }

    @Test
    void partial_result_backorder_lines_show_unfulfilled_quantities() {
        inventoryPort.setUnlimited(SKU_A);
        inventoryPort.setAvailable(SKU_B, 3);

        PrepareCorporateBulkOrderResult result = service.prepare(command(
                line(SKU_A, 10), line(SKU_B, 5)));

        // only SKU-B has backorder (SKU-A is fully available)
        assertThat(result.backorderLines()).hasSize(1);
        PrepareCorporateBulkOrderLineResult backorderB = result.backorderLines().get(0);
        assertThat(backorderB.sku()).isEqualTo(SKU_B);
        assertThat(backorderB.backorderQuantity()).isEqualTo(2);
    }

    // ── REJECT_NO_STOCK ───────────────────────────────────────────────────────

    @Test
    void prepare_returns_reject_when_no_stock_for_any_line() {
        // no availability set — stub returns 0 for all SKUs

        PrepareCorporateBulkOrderResult result = service.prepare(command(
                line(SKU_A, 10), line(SKU_B, 5)));

        assertThat(result.recommendedAction()).isEqualTo(RecommendedAction.REJECT_NO_STOCK);
        assertThat(result.allAvailable()).isFalse();
        assertThat(result.availableLines()).isEmpty();
        assertThat(result.backorderLines()).hasSize(2);
    }

    // ── Customer decision model ───────────────────────────────────────────────

    @Test
    void accepted_partial_decision_available_lines_give_quantities_for_submission() {
        inventoryPort.setAvailable(SKU_A, 10);  // fully available
        inventoryPort.setAvailable(SKU_B, 3);   // partially available (requested 5)
        inventoryPort.setAvailable(SKU_C, 0);   // completely unavailable

        PrepareCorporateBulkOrderResult result = service.prepare(command(
                line(SKU_A, 10), line(SKU_B, 5), line(SKU_C, 2)));

        // ACCEPT_PARTIAL: submit only availableLines at their availableQuantity
        assertThat(result.recommendedAction()).isEqualTo(RecommendedAction.ASK_CUSTOMER_ACCEPT_PARTIAL);

        // SKU-C has no stock → not in availableLines
        assertThat(result.availableLines()).hasSize(2);

        PrepareCorporateBulkOrderLineResult lineA = findBySku(result.availableLines(), SKU_A);
        PrepareCorporateBulkOrderLineResult lineB = findBySku(result.availableLines(), SKU_B);

        assertThat(lineA.availableQuantity()).isEqualTo(10);
        assertThat(lineB.availableQuantity()).isEqualTo(3);  // accepted quantity when ACCEPT_PARTIAL
    }

    @Test
    void customer_pre_saga_decisions_are_defined() {
        // Verify the enum values exist — they are used by the UI/controller layer
        assertThat(CustomerPreSagaDecision.values()).containsExactlyInAnyOrder(
                CustomerPreSagaDecision.ACCEPT_FULL,
                CustomerPreSagaDecision.ACCEPT_PARTIAL,
                CustomerPreSagaDecision.CANCEL);
    }

    // ── No Order created ──────────────────────────────────────────────────────

    @Test
    void prepare_does_not_require_order_repository() {
        // PrepareCorporateBulkOrderService only receives InventoryAvailabilityPort —
        // the constructor would fail at compile time if OrderRepository were required.
        inventoryPort.setUnlimited(SKU_A);

        PrepareCorporateBulkOrderResult result = service.prepare(command(line(SKU_A, 5)));

        // Result carries no orderId — no Order aggregate was created
        assertThat(result).isNotNull();
        assertThat(result.recommendedAction()).isEqualTo(RecommendedAction.PROCEED_FULL);
    }

    // ── InventoryAvailabilityPort is called ───────────────────────────────────

    @Test
    void result_reflects_inventory_availability_port_response() {
        inventoryPort.setAvailable(SKU_A, 7);

        PrepareCorporateBulkOrderResult result = service.prepare(command(line(SKU_A, 10)));

        PrepareCorporateBulkOrderLineResult lineA = result.requestedLines().get(0);
        assertThat(lineA.availableQuantity()).isEqualTo(7);
        assertThat(lineA.backorderQuantity()).isEqualTo(3);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void blank_customer_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PrepareCorporateBulkOrderCommand(
                        "", USER_ID, List.of(line(SKU_A, 1))))
                .withMessageContaining("customerId");
    }

    @Test
    void blank_user_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PrepareCorporateBulkOrderCommand(
                        CUSTOMER_ID, "", List.of(line(SKU_A, 1))))
                .withMessageContaining("submittedByUserId");
    }

    @Test
    void empty_lines_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PrepareCorporateBulkOrderCommand(
                        CUSTOMER_ID, USER_ID, List.of()))
                .withMessageContaining("lines");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PrepareCorporateBulkOrderCommand command(PrepareCorporateBulkOrderLineCommand... lines) {
        return new PrepareCorporateBulkOrderCommand(CUSTOMER_ID, USER_ID, List.of(lines));
    }

    private PrepareCorporateBulkOrderLineCommand line(String sku, int quantity) {
        return new PrepareCorporateBulkOrderLineCommand(sku, quantity);
    }

    private PrepareCorporateBulkOrderLineResult findBySku(
            List<PrepareCorporateBulkOrderLineResult> lines, String sku) {
        return lines.stream()
                .filter(l -> l.sku().equals(sku))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No result for SKU: " + sku));
    }
}
