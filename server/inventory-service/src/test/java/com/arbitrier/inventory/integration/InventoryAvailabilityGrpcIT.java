package com.arbitrier.inventory.integration;

import com.arbitrier.contracts.inventory.v1.AvailabilityStatus;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityRequest;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityResponse;
import com.arbitrier.contracts.inventory.v1.InventoryAvailabilityServiceGrpc;
import com.arbitrier.contracts.inventory.v1.RequestedItem;
import com.arbitrier.contracts.inventory.v1.UnavailabilityReason;
import com.arbitrier.inventory.adapter.inbound.grpc.InventoryAvailabilityGrpcRequestMapper;
import com.arbitrier.inventory.adapter.inbound.grpc.InventoryAvailabilityGrpcResponseMapper;
import com.arbitrier.inventory.adapter.inbound.grpc.InventoryAvailabilityGrpcService;
import com.arbitrier.inventory.adapter.inbound.grpc.InventoryGrpcExceptionMapper;
import com.arbitrier.inventory.adapter.outbound.ConfigurableInventoryAvailabilityQueryAdapter;
import com.arbitrier.inventory.application.service.CheckInventoryAvailabilityService;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * gRPC integration proof for {@link InventoryAvailabilityGrpcService}.
 *
 * <p>Uses an in-process gRPC server to exercise real protobuf serialization and server dispatch
 * without requiring a network port. The full adapter stack is exercised end-to-end.
 */
class InventoryAvailabilityGrpcIT {

    private static final String SERVER_NAME = "inventory-availability-test";

    private static ConfigurableInventoryAvailabilityQueryAdapter queryAdapter;
    private static Server grpcServer;
    private static ManagedChannel channel;
    private static InventoryAvailabilityServiceGrpc.InventoryAvailabilityServiceBlockingStub stub;

    @BeforeAll
    static void startServer() throws IOException {
        queryAdapter = new ConfigurableInventoryAvailabilityQueryAdapter();

        final var grpcService = new InventoryAvailabilityGrpcService(
                new CheckInventoryAvailabilityService(queryAdapter),
                new InventoryAvailabilityGrpcRequestMapper(),
                new InventoryAvailabilityGrpcResponseMapper(),
                new InventoryGrpcExceptionMapper());

        grpcServer = InProcessServerBuilder.forName(SERVER_NAME)
                .directExecutor()
                .addService(grpcService)
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(SERVER_NAME)
                .directExecutor()
                .build();

        stub = InventoryAvailabilityServiceGrpc.newBlockingStub(channel);
    }

    @AfterAll
    static void stopServer() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        grpcServer.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void available_scenario_returns_available_status() {
        queryAdapter.setAvailable("SKU-WIDGET-A", 100);

        final CheckAvailabilityResponse response = stub.checkAvailability(
                CheckAvailabilityRequest.newBuilder()
                        .setRequestId("it-req-001")
                        .addItems(RequestedItem.newBuilder()
                                .setProductId("SKU-WIDGET-A")
                                .setQuantity(50)
                                .build())
                        .build());

        assertThat(response.getStatus()).isEqualTo(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE);
        assertThat(response.getUnavailableItemsList()).isEmpty();
    }

    @Test
    void unavailable_scenario_returns_correct_details() {
        queryAdapter.setAvailable("SKU-WIDGET-B", 5);
        // SKU-MISSING is not registered

        final CheckAvailabilityResponse response = stub.checkAvailability(
                CheckAvailabilityRequest.newBuilder()
                        .setRequestId("it-req-002")
                        .addItems(RequestedItem.newBuilder()
                                .setProductId("SKU-WIDGET-B")
                                .setQuantity(10)
                                .build())
                        .addItems(RequestedItem.newBuilder()
                                .setProductId("SKU-MISSING")
                                .setQuantity(1)
                                .build())
                        .build());

        assertThat(response.getStatus()).isEqualTo(AvailabilityStatus.AVAILABILITY_STATUS_UNAVAILABLE);
        assertThat(response.getUnavailableItemsList()).hasSize(2);

        final var insufficientItem = response.getUnavailableItemsList().stream()
                .filter(i -> i.getProductId().equals("SKU-WIDGET-B"))
                .findFirst().orElseThrow();
        assertThat(insufficientItem.getReason())
                .isEqualTo(UnavailabilityReason.UNAVAILABILITY_REASON_INSUFFICIENT_STOCK);
        assertThat(insufficientItem.getRequestedQuantity()).isEqualTo(10);
        assertThat(insufficientItem.getAvailableQuantity()).isEqualTo(5);

        final var missingItem = response.getUnavailableItemsList().stream()
                .filter(i -> i.getProductId().equals("SKU-MISSING"))
                .findFirst().orElseThrow();
        assertThat(missingItem.getReason())
                .isEqualTo(UnavailabilityReason.UNAVAILABILITY_REASON_PRODUCT_NOT_FOUND);
        assertThat(missingItem.getAvailableQuantity()).isZero();
    }

    @Test
    void invalid_request_returns_invalid_argument_status() {
        // Empty items — will fail validation
        assertThatThrownBy(() -> stub.checkAvailability(
                CheckAvailabilityRequest.newBuilder()
                        .setRequestId("it-req-003")
                        // no items added
                        .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("INVALID_ARGUMENT");
    }
}
