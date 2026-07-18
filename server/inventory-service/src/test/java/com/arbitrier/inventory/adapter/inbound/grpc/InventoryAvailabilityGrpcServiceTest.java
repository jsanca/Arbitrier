package com.arbitrier.inventory.adapter.inbound.grpc;

import com.arbitrier.contracts.inventory.v1.AvailabilityStatus;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityRequest;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityResponse;
import com.arbitrier.contracts.inventory.v1.RequestedItem;
import com.arbitrier.contracts.inventory.v1.UnavailabilityReason;
import com.arbitrier.inventory.application.port.inbound.CheckInventoryAvailabilityUseCase;
import com.arbitrier.inventory.application.port.inbound.InventoryAvailabilityQuery;
import com.arbitrier.inventory.application.port.inbound.InventoryAvailabilityResult;
import com.arbitrier.inventory.application.port.inbound.InventoryUnavailabilityReason;
import com.arbitrier.inventory.application.port.inbound.UnavailableInventoryItem;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryAvailabilityGrpcServiceTest {

    @Mock
    private CheckInventoryAvailabilityUseCase useCase;

    @Mock
    private StreamObserver<CheckAvailabilityResponse> responseObserver;

    private InventoryAvailabilityGrpcService grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new InventoryAvailabilityGrpcService(
                useCase,
                new InventoryAvailabilityGrpcRequestMapper(),
                new InventoryAvailabilityGrpcResponseMapper(),
                new InventoryGrpcExceptionMapper());
    }

    @Test
    void valid_request_invokes_use_case() {
        when(useCase.check(any(InventoryAvailabilityQuery.class)))
                .thenReturn(new InventoryAvailabilityResult.Available());

        grpcService.checkAvailability(validRequest(), responseObserver);

        verify(useCase).check(any(InventoryAvailabilityQuery.class));
    }

    @Test
    void protobuf_request_maps_to_application_query() {
        when(useCase.check(any(InventoryAvailabilityQuery.class)))
                .thenReturn(new InventoryAvailabilityResult.Available());

        final var captor = ArgumentCaptor.forClass(InventoryAvailabilityQuery.class);

        grpcService.checkAvailability(validRequest(), responseObserver);

        verify(useCase).check(captor.capture());
        final var query = captor.getValue();
        assertThat(query.requestId()).isEqualTo("req-001");
        assertThat(query.items()).hasSize(1);
        assertThat(query.items().get(0).productId()).isEqualTo("SKU-A");
        assertThat(query.items().get(0).quantity()).isEqualTo(3);
    }

    @Test
    void available_result_maps_to_available_status() {
        when(useCase.check(any())).thenReturn(new InventoryAvailabilityResult.Available());

        grpcService.checkAvailability(validRequest(), responseObserver);

        final var captor = ArgumentCaptor.forClass(CheckAvailabilityResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE);
    }

    @Test
    void available_result_has_no_unavailable_items() {
        when(useCase.check(any())).thenReturn(new InventoryAvailabilityResult.Available());

        grpcService.checkAvailability(validRequest(), responseObserver);

        final var captor = ArgumentCaptor.forClass(CheckAvailabilityResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertThat(captor.getValue().getUnavailableItemsList()).isEmpty();
    }

    @Test
    void unavailable_result_maps_every_unavailable_item() {
        final var unavailable = new InventoryAvailabilityResult.Unavailable(List.of(
                new UnavailableInventoryItem("SKU-A", 5, 2, InventoryUnavailabilityReason.INSUFFICIENT_STOCK),
                new UnavailableInventoryItem("SKU-B", 1, 0, InventoryUnavailabilityReason.PRODUCT_NOT_FOUND)));
        when(useCase.check(any())).thenReturn(unavailable);

        grpcService.checkAvailability(validRequest(), responseObserver);

        final var captor = ArgumentCaptor.forClass(CheckAvailabilityResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertThat(captor.getValue().getUnavailableItemsList()).hasSize(2);
    }

    @Test
    void insufficient_stock_reason_maps_correctly() {
        final var unavailable = new InventoryAvailabilityResult.Unavailable(List.of(
                new UnavailableInventoryItem("SKU-A", 5, 2, InventoryUnavailabilityReason.INSUFFICIENT_STOCK)));
        when(useCase.check(any())).thenReturn(unavailable);

        grpcService.checkAvailability(validRequest(), responseObserver);

        final var captor = ArgumentCaptor.forClass(CheckAvailabilityResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertThat(captor.getValue().getUnavailableItems(0).getReason())
                .isEqualTo(UnavailabilityReason.UNAVAILABILITY_REASON_INSUFFICIENT_STOCK);
    }

    @Test
    void product_not_found_reason_maps_correctly() {
        final var unavailable = new InventoryAvailabilityResult.Unavailable(List.of(
                new UnavailableInventoryItem("SKU-X", 1, 0, InventoryUnavailabilityReason.PRODUCT_NOT_FOUND)));
        when(useCase.check(any())).thenReturn(unavailable);

        grpcService.checkAvailability(validRequest(), responseObserver);

        final var captor = ArgumentCaptor.forClass(CheckAvailabilityResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertThat(captor.getValue().getUnavailableItems(0).getReason())
                .isEqualTo(UnavailabilityReason.UNAVAILABILITY_REASON_PRODUCT_NOT_FOUND);
    }

    @Test
    void invalid_request_maps_to_invalid_argument() {
        when(useCase.check(any())).thenThrow(new IllegalArgumentException("blank request_id"));

        grpcService.checkAvailability(validRequest(), responseObserver);

        final var captor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) captor.getValue()).getStatus().getCode())
                .isEqualTo(Status.INVALID_ARGUMENT.getCode());
    }

    @Test
    void unexpected_null_pointer_maps_to_internal() {
        when(useCase.check(any())).thenThrow(new NullPointerException("unexpected null"));

        grpcService.checkAvailability(validRequest(), responseObserver);

        final var captor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) captor.getValue()).getStatus().getCode())
                .isEqualTo(Status.INTERNAL.getCode());
    }

    @Test
    void unexpected_failure_maps_to_internal() {
        when(useCase.check(any())).thenThrow(new RuntimeException("database down"));

        grpcService.checkAvailability(validRequest(), responseObserver);

        final var captor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) captor.getValue()).getStatus().getCode())
                .isEqualTo(Status.INTERNAL.getCode());
    }

    @Test
    void on_next_and_completed_occur_exactly_once_on_success() {
        when(useCase.check(any())).thenReturn(new InventoryAvailabilityResult.Available());

        grpcService.checkAvailability(validRequest(), responseObserver);

        verify(responseObserver, times(1)).onNext(any());
        verify(responseObserver, times(1)).onCompleted();
    }

    @Test
    void on_error_occurs_exactly_once_on_failure() {
        when(useCase.check(any())).thenThrow(new RuntimeException("failure"));

        grpcService.checkAvailability(validRequest(), responseObserver);

        verify(responseObserver, times(1)).onError(any());
    }

    @Test
    void successful_responses_never_use_unspecified() {
        when(useCase.check(any())).thenReturn(new InventoryAvailabilityResult.Available());

        grpcService.checkAvailability(validRequest(), responseObserver);

        final var captor = ArgumentCaptor.forClass(CheckAvailabilityResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertThat(captor.getValue().getStatus())
                .isNotEqualTo(AvailabilityStatus.AVAILABILITY_STATUS_UNSPECIFIED);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static CheckAvailabilityRequest validRequest() {
        return CheckAvailabilityRequest.newBuilder()
                .setRequestId("req-001")
                .addItems(RequestedItem.newBuilder()
                        .setProductId("SKU-A")
                        .setQuantity(3)
                        .build())
                .build();
    }
}
