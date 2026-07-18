package com.arbitrier.contracts;

import com.arbitrier.contracts.inventory.v1.AvailabilityStatus;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityRequest;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityResponse;
import com.arbitrier.contracts.inventory.v1.InventoryAvailabilityServiceGrpc;
import com.arbitrier.contracts.inventory.v1.RequestedItem;
import com.arbitrier.contracts.inventory.v1.UnavailabilityReason;
import com.arbitrier.contracts.inventory.v1.UnavailableItem;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract-level tests for the Inventory Availability gRPC contract.
 *
 * <p>Verifies generated types, round-trip serialization, and descriptor assertions.
 * Does not test server or client behavior.
 */
class InventoryAvailabilityContractTest {

    // ── generated types ───────────────────────────────────────────────────────

    @Test
    void request_can_contain_multiple_items() {
        CheckAvailabilityRequest request = CheckAvailabilityRequest.newBuilder()
                .setRequestId("req-001")
                .addItems(RequestedItem.newBuilder().setProductId("SKU-A").setQuantity(10).build())
                .addItems(RequestedItem.newBuilder().setProductId("SKU-B").setQuantity(5).build())
                .addItems(RequestedItem.newBuilder().setProductId("SKU-C").setQuantity(1).build())
                .build();

        assertThat(request.getItemsCount()).isEqualTo(3);
        assertThat(request.getItems(0).getProductId()).isEqualTo("SKU-A");
        assertThat(request.getItems(0).getQuantity()).isEqualTo(10);
        assertThat(request.getItems(1).getProductId()).isEqualTo("SKU-B");
        assertThat(request.getItems(2).getProductId()).isEqualTo("SKU-C");
    }

    @Test
    void response_can_represent_available() {
        CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE)
                .build();

        assertThat(response.getStatus()).isEqualTo(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE);
        assertThat(response.getUnavailableItemsList()).isEmpty();
    }

    @Test
    void response_can_represent_unavailable() {
        CheckAvailabilityResponse response = CheckAvailabilityResponse.newBuilder()
                .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_UNAVAILABLE)
                .addUnavailableItems(UnavailableItem.newBuilder()
                        .setProductId("SKU-A")
                        .setRequestedQuantity(10)
                        .setAvailableQuantity(3)
                        .setReason(UnavailabilityReason.UNAVAILABILITY_REASON_INSUFFICIENT_STOCK)
                        .build())
                .build();

        assertThat(response.getStatus()).isEqualTo(AvailabilityStatus.AVAILABILITY_STATUS_UNAVAILABLE);
        assertThat(response.getUnavailableItemsCount()).isEqualTo(1);
    }

    @Test
    void unavailable_item_details_are_preserved() {
        UnavailableItem item = UnavailableItem.newBuilder()
                .setProductId("SKU-X")
                .setRequestedQuantity(20)
                .setAvailableQuantity(0)
                .setReason(UnavailabilityReason.UNAVAILABILITY_REASON_PRODUCT_NOT_FOUND)
                .build();

        assertThat(item.getProductId()).isEqualTo("SKU-X");
        assertThat(item.getRequestedQuantity()).isEqualTo(20);
        assertThat(item.getAvailableQuantity()).isEqualTo(0);
        assertThat(item.getReason()).isEqualTo(UnavailabilityReason.UNAVAILABILITY_REASON_PRODUCT_NOT_FOUND);
    }

    @Test
    void availability_status_enum_zero_value_is_unspecified() {
        assertThat(AvailabilityStatus.AVAILABILITY_STATUS_UNSPECIFIED.getNumber()).isEqualTo(0);
    }

    @Test
    void unavailability_reason_enum_zero_value_is_unspecified() {
        assertThat(UnavailabilityReason.UNAVAILABILITY_REASON_UNSPECIFIED.getNumber()).isEqualTo(0);
    }

    @Test
    void generated_grpc_service_type_exists() {
        assertThat(InventoryAvailabilityServiceGrpc.getServiceDescriptor()).isNotNull();
    }

    // ── serialization round-trips ─────────────────────────────────────────────

    @Test
    void request_survives_serialize_parse_round_trip() throws InvalidProtocolBufferException {
        CheckAvailabilityRequest original = CheckAvailabilityRequest.newBuilder()
                .setRequestId("round-trip-req")
                .addItems(RequestedItem.newBuilder().setProductId("SKU-RT").setQuantity(7).build())
                .build();

        byte[] bytes = original.toByteArray();
        CheckAvailabilityRequest parsed = CheckAvailabilityRequest.parseFrom(bytes);

        assertThat(parsed.getRequestId()).isEqualTo("round-trip-req");
        assertThat(parsed.getItemsCount()).isEqualTo(1);
        assertThat(parsed.getItems(0).getProductId()).isEqualTo("SKU-RT");
        assertThat(parsed.getItems(0).getQuantity()).isEqualTo(7);
    }

    @Test
    void available_response_survives_round_trip() throws InvalidProtocolBufferException {
        CheckAvailabilityResponse original = CheckAvailabilityResponse.newBuilder()
                .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE)
                .build();

        CheckAvailabilityResponse parsed = CheckAvailabilityResponse.parseFrom(original.toByteArray());

        assertThat(parsed.getStatus()).isEqualTo(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE);
        assertThat(parsed.getUnavailableItemsList()).isEmpty();
    }

    @Test
    void unavailable_response_survives_round_trip() throws InvalidProtocolBufferException {
        CheckAvailabilityResponse original = CheckAvailabilityResponse.newBuilder()
                .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_UNAVAILABLE)
                .addUnavailableItems(UnavailableItem.newBuilder()
                        .setProductId("SKU-RT")
                        .setRequestedQuantity(15)
                        .setAvailableQuantity(4)
                        .setReason(UnavailabilityReason.UNAVAILABILITY_REASON_INSUFFICIENT_STOCK)
                        .build())
                .build();

        CheckAvailabilityResponse parsed = CheckAvailabilityResponse.parseFrom(original.toByteArray());

        assertThat(parsed.getStatus()).isEqualTo(AvailabilityStatus.AVAILABILITY_STATUS_UNAVAILABLE);
        assertThat(parsed.getUnavailableItemsCount()).isEqualTo(1);
        UnavailableItem parsedItem = parsed.getUnavailableItems(0);
        assertThat(parsedItem.getProductId()).isEqualTo("SKU-RT");
        assertThat(parsedItem.getRequestedQuantity()).isEqualTo(15);
        assertThat(parsedItem.getAvailableQuantity()).isEqualTo(4);
        assertThat(parsedItem.getReason())
                .isEqualTo(UnavailabilityReason.UNAVAILABILITY_REASON_INSUFFICIENT_STOCK);
    }

    // ── descriptor assertions ─────────────────────────────────────────────────

    @Test
    void service_name_is_fully_qualified() {
        String serviceName = InventoryAvailabilityServiceGrpc.getServiceDescriptor().getName();
        assertThat(serviceName).isEqualTo("arbitrier.inventory.v1.InventoryAvailabilityService");
    }

    @Test
    void check_availability_method_exists() {
        MethodDescriptor<CheckAvailabilityRequest, CheckAvailabilityResponse> method =
                InventoryAvailabilityServiceGrpc.getCheckAvailabilityMethod();
        assertThat(method).isNotNull();
    }

    @Test
    void check_availability_is_unary() {
        MethodDescriptor<CheckAvailabilityRequest, CheckAvailabilityResponse> method =
                InventoryAvailabilityServiceGrpc.getCheckAvailabilityMethod();
        assertThat(method.getType()).isEqualTo(MethodDescriptor.MethodType.UNARY);
    }

    @Test
    void check_availability_full_method_name() {
        MethodDescriptor<CheckAvailabilityRequest, CheckAvailabilityResponse> method =
                InventoryAvailabilityServiceGrpc.getCheckAvailabilityMethod();
        assertThat(method.getFullMethodName())
                .isEqualTo("arbitrier.inventory.v1.InventoryAvailabilityService/CheckAvailability");
    }
}
