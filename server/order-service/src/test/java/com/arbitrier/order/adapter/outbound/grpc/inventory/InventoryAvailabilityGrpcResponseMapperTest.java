package com.arbitrier.order.adapter.outbound.grpc.inventory;

import com.arbitrier.contracts.inventory.v1.AvailabilityStatus;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityResponse;
import com.arbitrier.contracts.inventory.v1.UnavailableItem;
import com.arbitrier.contracts.inventory.v1.UnavailabilityReason;
import com.arbitrier.order.application.port.outbound.AvailabilityLineQuery;
import com.arbitrier.order.application.port.outbound.AvailabilityLineResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryAvailabilityGrpcResponseMapperTest {

    private final InventoryAvailabilityGrpcResponseMapper mapper = new InventoryAvailabilityGrpcResponseMapper();

    private static final List<AvailabilityLineQuery> TWO_ITEMS = List.of(
            new AvailabilityLineQuery("SKU-001", 7),
            new AvailabilityLineQuery("SKU-002", 3));

    // ── AVAILABLE ──────────────────────────────────────────────────────────────

    @Test
    void available_maps_each_line_with_requested_quantity() {
        final CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE)
                .build();

        final List<AvailabilityLineResponse> result = mapper.toLines(response, TWO_ITEMS);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).sku()).isEqualTo("SKU-001");
        assertThat(result.get(0).availableQuantity()).isEqualTo(7);
        assertThat(result.get(1).sku()).isEqualTo("SKU-002");
        assertThat(result.get(1).availableQuantity()).isEqualTo(3);
    }

    @Test
    void available_with_non_empty_unavailable_items_raises_protocol_exception() {
        final CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE)
                .addUnavailableItems(UnavailableItem.newBuilder()
                        .setProductId("SKU-001")
                        .setRequestedQuantity(7)
                        .setAvailableQuantity(0)
                        .setReason(UnavailabilityReason.UNAVAILABILITY_REASON_PRODUCT_NOT_FOUND)
                        .build())
                .build();

        assertThatThrownBy(() -> mapper.toLines(response, TWO_ITEMS))
                .isInstanceOf(InventoryAvailabilityProtocolException.class)
                .hasMessageContaining("protocol violation");
    }

    // ── UNAVAILABLE ────────────────────────────────────────────────────────────

    @Test
    void unavailable_item_uses_server_available_quantity() {
        final CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_UNAVAILABLE)
                .addUnavailableItems(UnavailableItem.newBuilder()
                        .setProductId("SKU-001")
                        .setRequestedQuantity(7)
                        .setAvailableQuantity(4)
                        .setReason(UnavailabilityReason.UNAVAILABILITY_REASON_INSUFFICIENT_STOCK)
                        .build())
                .build();

        final List<AvailabilityLineResponse> result = mapper.toLines(response,
                List.of(new AvailabilityLineQuery("SKU-001", 7)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sku()).isEqualTo("SKU-001");
        assertThat(result.get(0).availableQuantity()).isEqualTo(4);
    }

    @Test
    void product_not_found_returns_zero_available_quantity() {
        final CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_UNAVAILABLE)
                .addUnavailableItems(UnavailableItem.newBuilder()
                        .setProductId("SKU-MISSING")
                        .setRequestedQuantity(1)
                        .setAvailableQuantity(0)
                        .setReason(UnavailabilityReason.UNAVAILABILITY_REASON_PRODUCT_NOT_FOUND)
                        .build())
                .build();

        final List<AvailabilityLineResponse> result = mapper.toLines(response,
                List.of(new AvailabilityLineQuery("SKU-MISSING", 1)));

        assertThat(result.get(0).availableQuantity()).isZero();
    }

    @Test
    void partial_unavailability_available_items_use_requested_quantity() {
        // SKU-001 is unavailable, SKU-002 is not in unavailable_items (was available)
        final CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_UNAVAILABLE)
                .addUnavailableItems(UnavailableItem.newBuilder()
                        .setProductId("SKU-001")
                        .setRequestedQuantity(7)
                        .setAvailableQuantity(3)
                        .setReason(UnavailabilityReason.UNAVAILABILITY_REASON_INSUFFICIENT_STOCK)
                        .build())
                .build();

        final List<AvailabilityLineResponse> result = mapper.toLines(response, TWO_ITEMS);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).availableQuantity()).isEqualTo(3);   // SKU-001: unavailable
        assertThat(result.get(1).availableQuantity()).isEqualTo(3);   // SKU-002: available → requestedQty
    }

    @Test
    void requested_quantity_is_preserved_in_unavailable_item() {
        final CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_UNAVAILABLE)
                .addUnavailableItems(UnavailableItem.newBuilder()
                        .setProductId("SKU-001")
                        .setRequestedQuantity(8)
                        .setAvailableQuantity(7)
                        .setReason(UnavailabilityReason.UNAVAILABILITY_REASON_INSUFFICIENT_STOCK)
                        .build())
                .build();

        // The available_quantity from the response (7) is returned, not requestedQuantity (8)
        final List<AvailabilityLineResponse> result = mapper.toLines(response,
                List.of(new AvailabilityLineQuery("SKU-001", 8)));

        assertThat(result.get(0).availableQuantity()).isEqualTo(7);
    }

    // ── UNSPECIFIED ────────────────────────────────────────────────────────────

    @Test
    void unspecified_status_raises_protocol_exception() {
        final CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_UNSPECIFIED)
                .build();

        assertThatThrownBy(() -> mapper.toLines(response, TWO_ITEMS))
                .isInstanceOf(InventoryAvailabilityProtocolException.class)
                .hasMessageContaining("AVAILABILITY_STATUS_UNSPECIFIED");
    }

    @Test
    void unspecified_does_not_produce_business_unavailability() {
        final CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_UNSPECIFIED)
                .build();

        assertThatThrownBy(() -> mapper.toLines(response, TWO_ITEMS))
                .isInstanceOf(InventoryAvailabilityProtocolException.class)
                .isNotInstanceOf(IllegalArgumentException.class);
    }
}
