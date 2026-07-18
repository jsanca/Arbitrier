package com.arbitrier.order.adapter.outbound.grpc.inventory;

/**
 * Thrown when Inventory Service is temporarily unavailable (gRPC status {@code UNAVAILABLE}).
 *
 * <p>Layer: adapter/outbound/grpc/inventory
 * <p>Module: order-service
 */
public class InventoryAvailabilityRemoteUnavailableException extends InventoryAvailabilityException {

    public InventoryAvailabilityRemoteUnavailableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
