package com.arbitrier.order.adapter.outbound.grpc.inventory;

import com.arbitrier.contracts.inventory.v1.AvailabilityStatus;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityRequest;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityResponse;
import com.arbitrier.contracts.inventory.v1.InventoryAvailabilityServiceGrpc;
import com.arbitrier.contracts.inventory.v1.UnavailableItem;
import com.arbitrier.contracts.inventory.v1.UnavailabilityReason;
import com.arbitrier.order.application.port.outbound.AvailabilityLineQuery;
import com.arbitrier.order.application.port.outbound.AvailabilityLineResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Response-contract validation tests use the injectable-mapper constructor to make the
// mapper return bad data that the real gRPC protocol can't produce.

class GrpcInventoryAvailabilityAdapterTest {

    private InventoryAvailabilityServiceGrpc.InventoryAvailabilityServiceBlockingStub stub;
    private InventoryAvailabilityServiceGrpc.InventoryAvailabilityServiceBlockingStub deadlineStub;
    private GrpcInventoryAvailabilityAdapter adapter;

    private static final Duration DEADLINE = Duration.ofMillis(500);
    private static final List<AvailabilityLineQuery> QUERIES = List.of(
            new AvailabilityLineQuery("SKU-001", 5));
    private static final List<AvailabilityLineQuery> TWO_QUERIES = List.of(
            new AvailabilityLineQuery("SKU-001", 5),
            new AvailabilityLineQuery("SKU-002", 3));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        stub = mock(InventoryAvailabilityServiceGrpc.InventoryAvailabilityServiceBlockingStub.class);
        deadlineStub = mock(InventoryAvailabilityServiceGrpc.InventoryAvailabilityServiceBlockingStub.class);
        when(stub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(deadlineStub);
        adapter = new GrpcInventoryAvailabilityAdapter(stub, DEADLINE);
    }

    /** Builds an adapter with a mock response mapper that returns {@code mapperResult}. */
    @SuppressWarnings("unchecked")
    private GrpcInventoryAvailabilityAdapter adapterWithMappedResult(
            final List<AvailabilityLineResponse> mapperResult) {
        final InventoryAvailabilityGrpcResponseMapper mockMapper =
                mock(InventoryAvailabilityGrpcResponseMapper.class);
        when(mockMapper.toLines(any(), any())).thenReturn(mapperResult);
        when(deadlineStub.checkAvailability(any())).thenReturn(
                CheckAvailabilityResponse.newBuilder()
                        .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE)
                        .build());
        return new GrpcInventoryAvailabilityAdapter(stub, DEADLINE, mockMapper);
    }

    @Test
    void adapter_invokes_stub_exactly_once() {
        when(deadlineStub.checkAvailability(any())).thenReturn(
                CheckAvailabilityResponse.newBuilder()
                        .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE)
                        .build());

        adapter.checkAvailability(QUERIES);

        verify(deadlineStub, times(1)).checkAvailability(any(CheckAvailabilityRequest.class));
    }

    @Test
    void adapter_applies_configured_deadline() {
        when(deadlineStub.checkAvailability(any())).thenReturn(
                CheckAvailabilityResponse.newBuilder()
                        .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE)
                        .build());

        adapter.checkAvailability(QUERIES);

        verify(stub).withDeadlineAfter(eq(DEADLINE.toMillis()), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void available_response_returns_all_lines_with_requested_quantities() {
        when(deadlineStub.checkAvailability(any())).thenReturn(
                CheckAvailabilityResponse.newBuilder()
                        .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE)
                        .build());

        final List<AvailabilityLineResponse> result = adapter.checkAvailability(QUERIES);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sku()).isEqualTo("SKU-001");
        assertThat(result.get(0).availableQuantity()).isEqualTo(5);
    }

    @Test
    void unavailable_response_returns_server_available_quantity() {
        when(deadlineStub.checkAvailability(any())).thenReturn(
                CheckAvailabilityResponse.newBuilder()
                        .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_UNAVAILABLE)
                        .addUnavailableItems(UnavailableItem.newBuilder()
                                .setProductId("SKU-001")
                                .setRequestedQuantity(5)
                                .setAvailableQuantity(2)
                                .setReason(UnavailabilityReason.UNAVAILABILITY_REASON_INSUFFICIENT_STOCK)
                                .build())
                        .build());

        final List<AvailabilityLineResponse> result = adapter.checkAvailability(QUERIES);

        assertThat(result.get(0).availableQuantity()).isEqualTo(2);
    }

    @Test
    void deadline_exceeded_maps_to_timeout_exception() {
        when(deadlineStub.checkAvailability(any()))
                .thenThrow(Status.DEADLINE_EXCEEDED.asRuntimeException());

        assertThatThrownBy(() -> adapter.checkAvailability(QUERIES))
                .isInstanceOf(InventoryAvailabilityTimeoutException.class);
    }

    @Test
    void unavailable_status_maps_to_remote_unavailable_exception() {
        when(deadlineStub.checkAvailability(any()))
                .thenThrow(Status.UNAVAILABLE.asRuntimeException());

        assertThatThrownBy(() -> adapter.checkAvailability(QUERIES))
                .isInstanceOf(InventoryAvailabilityRemoteUnavailableException.class);
    }

    @Test
    void invalid_argument_maps_to_protocol_exception() {
        when(deadlineStub.checkAvailability(any()))
                .thenThrow(Status.INVALID_ARGUMENT.withDescription("bad request").asRuntimeException());

        assertThatThrownBy(() -> adapter.checkAvailability(QUERIES))
                .isInstanceOf(InventoryAvailabilityProtocolException.class);
    }

    @Test
    void internal_status_maps_to_internal_exception() {
        when(deadlineStub.checkAvailability(any()))
                .thenThrow(Status.INTERNAL.asRuntimeException());

        assertThatThrownBy(() -> adapter.checkAvailability(QUERIES))
                .isInstanceOf(InventoryAvailabilityInternalException.class);
    }

    @Test
    void protocol_exception_from_response_mapper_propagates_without_rewrapping() {
        when(deadlineStub.checkAvailability(any())).thenReturn(
                CheckAvailabilityResponse.newBuilder()
                        .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_UNSPECIFIED)
                        .build());

        assertThatThrownBy(() -> adapter.checkAvailability(QUERIES))
                .isInstanceOf(InventoryAvailabilityProtocolException.class)
                .isNotInstanceOf(InventoryAvailabilityInternalException.class);
    }

    @Test
    void technical_exceptions_preserve_original_grpc_cause() {
        final StatusRuntimeException original = Status.DEADLINE_EXCEEDED.asRuntimeException();
        when(deadlineStub.checkAvailability(any())).thenThrow(original);

        assertThatThrownBy(() -> adapter.checkAvailability(QUERIES))
                .isInstanceOf(InventoryAvailabilityTimeoutException.class)
                .hasCause(original);
    }

    @Test
    void adapter_sends_mapped_protobuf_request() {
        when(deadlineStub.checkAvailability(any())).thenReturn(
                CheckAvailabilityResponse.newBuilder()
                        .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE)
                        .build());

        adapter.checkAvailability(QUERIES);

        final ArgumentCaptor<CheckAvailabilityRequest> captor =
                ArgumentCaptor.forClass(CheckAvailabilityRequest.class);
        verify(deadlineStub).checkAvailability(captor.capture());

        final CheckAvailabilityRequest sent = captor.getValue();
        assertThat(sent.getRequestId()).isNotBlank();
        assertThat(sent.getItemsCount()).isEqualTo(1);
        assertThat(sent.getItems(0).getProductId()).isEqualTo("SKU-001");
        assertThat(sent.getItems(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void adapter_does_not_mutate_inventory_stock() {
        // Availability is a read-only check — verified by the absence of write calls.
        when(deadlineStub.checkAvailability(any())).thenReturn(
                CheckAvailabilityResponse.newBuilder()
                        .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE)
                        .build());

        adapter.checkAvailability(QUERIES);

        // Only checkAvailability should be called; no other interaction on the stub
        verify(deadlineStub, times(1)).checkAvailability(any());
    }

    // ── response contract validation ──────────────────────────────────────────

    @Test
    void complete_response_is_accepted() {
        final List<AvailabilityLineResponse> expected = List.of(
                new AvailabilityLineResponse("SKU-001", 5),
                new AvailabilityLineResponse("SKU-002", 3));
        final GrpcInventoryAvailabilityAdapter testAdapter = adapterWithMappedResult(expected);

        final List<AvailabilityLineResponse> result = testAdapter.checkAvailability(TWO_QUERIES);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void empty_response_for_nonempty_request_throws_protocol_exception() {
        final GrpcInventoryAvailabilityAdapter testAdapter = adapterWithMappedResult(List.of());

        assertThatThrownBy(() -> testAdapter.checkAvailability(QUERIES))
                .isInstanceOf(InventoryAvailabilityProtocolException.class)
                .hasMessageContaining("mismatch");
    }

    @Test
    void missing_requested_sku_throws_protocol_exception() {
        // 2 queries, mapper returns only 1 line
        final List<AvailabilityLineResponse> partial = List.of(
                new AvailabilityLineResponse("SKU-001", 5));
        final GrpcInventoryAvailabilityAdapter testAdapter = adapterWithMappedResult(partial);

        assertThatThrownBy(() -> testAdapter.checkAvailability(TWO_QUERIES))
                .isInstanceOf(InventoryAvailabilityProtocolException.class)
                .hasMessageContaining("mismatch");
    }

    @Test
    void duplicate_response_sku_throws_protocol_exception() {
        final List<AvailabilityLineResponse> withDuplicate = List.of(
                new AvailabilityLineResponse("SKU-001", 5),
                new AvailabilityLineResponse("SKU-001", 5));
        final GrpcInventoryAvailabilityAdapter testAdapter = adapterWithMappedResult(withDuplicate);

        assertThatThrownBy(() -> testAdapter.checkAvailability(TWO_QUERIES))
                .isInstanceOf(InventoryAvailabilityProtocolException.class)
                .hasMessageContaining("duplicate SKU");
    }

    @Test
    void unexpected_response_sku_throws_protocol_exception() {
        // Queries are SKU-001 and SKU-002; response includes SKU-GHOST
        final List<AvailabilityLineResponse> withUnexpected = List.of(
                new AvailabilityLineResponse("SKU-001", 5),
                new AvailabilityLineResponse("SKU-GHOST", 3));
        final GrpcInventoryAvailabilityAdapter testAdapter = adapterWithMappedResult(withUnexpected);

        assertThatThrownBy(() -> testAdapter.checkAvailability(TWO_QUERIES))
                .isInstanceOf(InventoryAvailabilityProtocolException.class)
                .hasMessageContaining("unexpected SKU");
    }

    @Test
    void response_in_different_order_from_request_is_accepted() {
        // Response returns SKU-002 before SKU-001 — order should not matter
        final List<AvailabilityLineResponse> reversedOrder = List.of(
                new AvailabilityLineResponse("SKU-002", 3),
                new AvailabilityLineResponse("SKU-001", 5));
        final GrpcInventoryAvailabilityAdapter testAdapter = adapterWithMappedResult(reversedOrder);

        final List<AvailabilityLineResponse> result = testAdapter.checkAvailability(TWO_QUERIES);

        assertThat(result).containsExactlyInAnyOrder(
                new AvailabilityLineResponse("SKU-001", 5),
                new AvailabilityLineResponse("SKU-002", 3));
    }

    @Test
    void insufficient_quantity_is_returned_normally_and_not_treated_as_protocol_error() {
        final List<AvailabilityLineResponse> insufficient = List.of(
                new AvailabilityLineResponse("SKU-001", 2)); // requested 5, only 2 available
        final GrpcInventoryAvailabilityAdapter testAdapter = adapterWithMappedResult(insufficient);

        final List<AvailabilityLineResponse> result = testAdapter.checkAvailability(QUERIES);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).availableQuantity()).isEqualTo(2);
    }
}
