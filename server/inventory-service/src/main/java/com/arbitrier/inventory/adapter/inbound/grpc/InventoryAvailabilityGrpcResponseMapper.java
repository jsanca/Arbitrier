package com.arbitrier.inventory.adapter.inbound.grpc;

import com.arbitrier.contracts.inventory.v1.AvailabilityStatus;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityResponse;
import com.arbitrier.contracts.inventory.v1.UnavailabilityReason;
import com.arbitrier.contracts.inventory.v1.UnavailableItem;
import com.arbitrier.inventory.application.port.inbound.InventoryAvailabilityResult;
import com.arbitrier.inventory.application.port.inbound.InventoryUnavailabilityReason;
import com.arbitrier.inventory.application.port.inbound.UnavailableInventoryItem;

/**
 * Maps an application {@link InventoryAvailabilityResult} to a protobuf
 * {@link CheckAvailabilityResponse}.
 *
 * <p>Successful business results are never encoded as gRPC status errors — business
 * unavailability maps to {@code AVAILABILITY_STATUS_UNAVAILABLE}, not to an error status.
 *
 * <p>Layer: adapter/inbound/grpc
 * <p>Module: inventory-service
 */
public class InventoryAvailabilityGrpcResponseMapper {

    /**
     * Converts an application result to a protobuf response.
     *
     * @param result the application result; must not be null
     * @return the corresponding protobuf response
     */
    public CheckAvailabilityResponse toResponse(final InventoryAvailabilityResult result) {
        return switch (result) {
            case InventoryAvailabilityResult.Available ignored ->
                    CheckAvailabilityResponse.newBuilder()
                            .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE)
                            .build();

            case InventoryAvailabilityResult.Unavailable unavailable ->
                    CheckAvailabilityResponse.newBuilder()
                            .setStatus(AvailabilityStatus.AVAILABILITY_STATUS_UNAVAILABLE)
                            .addAllUnavailableItems(unavailable.items().stream()
                                    .map(this::toUnavailableItem)
                                    .toList())
                            .build();
        };
    }

    private UnavailableItem toUnavailableItem(final UnavailableInventoryItem item) {
        return UnavailableItem.newBuilder()
                .setProductId(item.productId())
                .setRequestedQuantity(item.requestedQuantity())
                .setAvailableQuantity(item.availableQuantity())
                .setReason(toReason(item.reason()))
                .build();
    }

    private UnavailabilityReason toReason(final InventoryUnavailabilityReason reason) {
        return switch (reason) {
            case INSUFFICIENT_STOCK -> UnavailabilityReason.UNAVAILABILITY_REASON_INSUFFICIENT_STOCK;
            case PRODUCT_NOT_FOUND -> UnavailabilityReason.UNAVAILABILITY_REASON_PRODUCT_NOT_FOUND;
        };
    }
}
