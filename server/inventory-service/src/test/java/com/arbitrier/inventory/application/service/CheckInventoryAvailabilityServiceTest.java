package com.arbitrier.inventory.application.service;

import com.arbitrier.inventory.adapter.outbound.ConfigurableInventoryAvailabilityQueryAdapter;
import com.arbitrier.inventory.application.port.inbound.CheckInventoryAvailabilityUseCase;
import com.arbitrier.inventory.application.port.inbound.InventoryAvailabilityQuery;
import com.arbitrier.inventory.application.port.inbound.InventoryAvailabilityResult;
import com.arbitrier.inventory.application.port.inbound.InventoryUnavailabilityReason;
import com.arbitrier.inventory.application.port.inbound.RequestedInventoryItem;
import com.arbitrier.inventory.application.port.inbound.UnavailableInventoryItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckInventoryAvailabilityServiceTest {

    private ConfigurableInventoryAvailabilityQueryAdapter queryAdapter;
    private CheckInventoryAvailabilityUseCase useCase;

    @BeforeEach
    void setUp() {
        queryAdapter = new ConfigurableInventoryAvailabilityQueryAdapter();
        useCase = new CheckInventoryAvailabilityService(queryAdapter);
    }

    @Test
    void all_products_available_returns_available() {
        queryAdapter.setAvailable("SKU-A", 10);
        queryAdapter.setAvailable("SKU-B", 5);

        final var result = useCase.check(query("req-1",
                item("SKU-A", 5), item("SKU-B", 3)));

        assertThat(result).isInstanceOf(InventoryAvailabilityResult.Available.class);
    }

    @Test
    void one_insufficient_product_returns_unavailable() {
        queryAdapter.setAvailable("SKU-A", 3);

        final var result = useCase.check(query("req-2", item("SKU-A", 5)));

        assertThat(result).isInstanceOf(InventoryAvailabilityResult.Unavailable.class);
        final var unavailable = (InventoryAvailabilityResult.Unavailable) result;
        assertThat(unavailable.items()).hasSize(1);
        assertThat(unavailable.items().get(0).reason())
                .isEqualTo(InventoryUnavailabilityReason.INSUFFICIENT_STOCK);
    }

    @Test
    void several_insufficient_products_are_all_reported() {
        queryAdapter.setAvailable("SKU-A", 2);
        queryAdapter.setAvailable("SKU-B", 1);

        final var result = useCase.check(query("req-3",
                item("SKU-A", 5), item("SKU-B", 3)));

        assertThat(result).isInstanceOf(InventoryAvailabilityResult.Unavailable.class);
        final var unavailable = (InventoryAvailabilityResult.Unavailable) result;
        assertThat(unavailable.items()).hasSize(2);
    }

    @Test
    void missing_product_returns_product_not_found() {
        // no stock registered for SKU-X

        final var result = useCase.check(query("req-4", item("SKU-X", 1)));

        assertThat(result).isInstanceOf(InventoryAvailabilityResult.Unavailable.class);
        final var unavailable = (InventoryAvailabilityResult.Unavailable) result;
        assertThat(unavailable.items()).hasSize(1);
        assertThat(unavailable.items().get(0).reason())
                .isEqualTo(InventoryUnavailabilityReason.PRODUCT_NOT_FOUND);
        assertThat(unavailable.items().get(0).availableQuantity()).isZero();
    }

    @Test
    void missing_and_insufficient_products_coexist_in_result() {
        queryAdapter.setAvailable("SKU-A", 2);
        // SKU-MISSING has no stock entry

        final var result = useCase.check(query("req-5",
                item("SKU-A", 10), item("SKU-MISSING", 1)));

        assertThat(result).isInstanceOf(InventoryAvailabilityResult.Unavailable.class);
        final var unavailable = (InventoryAvailabilityResult.Unavailable) result;

        assertThat(unavailable.items()).hasSize(2);
        assertThat(unavailable.items()).extracting(UnavailableInventoryItem::reason)
                .containsExactlyInAnyOrder(
                        InventoryUnavailabilityReason.INSUFFICIENT_STOCK,
                        InventoryUnavailabilityReason.PRODUCT_NOT_FOUND);
    }

    @Test
    void available_quantity_is_preserved_in_result() {
        queryAdapter.setAvailable("SKU-A", 7);

        final var result = useCase.check(query("req-6", item("SKU-A", 10)));

        final var unavailable = (InventoryAvailabilityResult.Unavailable) result;
        assertThat(unavailable.items().get(0).availableQuantity()).isEqualTo(7);
    }

    @Test
    void requested_quantity_is_preserved_in_result() {
        queryAdapter.setAvailable("SKU-A", 1);

        final var result = useCase.check(query("req-7", item("SKU-A", 10)));

        final var unavailable = (InventoryAvailabilityResult.Unavailable) result;
        assertThat(unavailable.items().get(0).requestedQuantity()).isEqualTo(10);
    }

    @Test
    void empty_items_are_rejected() {
        assertThatThrownBy(() ->
                useCase.check(new InventoryAvailabilityQuery("req-8", List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void non_positive_quantity_is_rejected() {
        assertThatThrownBy(() -> new RequestedInventoryItem("SKU-A", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RequestedInventoryItem("SKU-A", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blank_product_id_is_rejected() {
        assertThatThrownBy(() -> new RequestedInventoryItem("", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RequestedInventoryItem("  ", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicate_product_ids_are_rejected() {
        assertThatThrownBy(() ->
                useCase.check(query("req-9", item("SKU-A", 1), item("SKU-A", 2))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU-A");
    }

    @Test
    void query_does_not_mutate_inventory() {
        queryAdapter.setAvailable("SKU-A", 5);
        useCase.check(query("req-10", item("SKU-A", 3)));

        // Stock should still be 5 after the read-only check
        final var secondResult = useCase.check(query("req-11", item("SKU-A", 5)));
        assertThat(secondResult).isInstanceOf(InventoryAvailabilityResult.Available.class);
    }

    @Test
    void query_port_is_invoked_with_all_requested_product_ids() {
        queryAdapter.setAvailable("SKU-A", 10);
        queryAdapter.setAvailable("SKU-B", 10);
        queryAdapter.setAvailable("SKU-C", 10);

        final var result = useCase.check(query("req-12",
                item("SKU-A", 1), item("SKU-B", 1), item("SKU-C", 1)));

        assertThat(result).isInstanceOf(InventoryAvailabilityResult.Available.class);
    }

    @Test
    void exact_available_quantity_is_available() {
        queryAdapter.setAvailable("SKU-A", 5);

        final var result = useCase.check(query("req-13", item("SKU-A", 5)));

        assertThat(result).isInstanceOf(InventoryAvailabilityResult.Available.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static InventoryAvailabilityQuery query(final String requestId,
                                                    final RequestedInventoryItem... items) {
        return new InventoryAvailabilityQuery(requestId, List.of(items));
    }

    private static RequestedInventoryItem item(final String productId, final int quantity) {
        return new RequestedInventoryItem(productId, quantity);
    }
}
