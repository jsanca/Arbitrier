package com.arbitrier.order.adapter.outbound.grpc.inventory;

/**
 * Thrown when the Inventory gRPC call exceeds its configured deadline or is cancelled.
 *
 * <p>Maps gRPC status codes: {@code DEADLINE_EXCEEDED}, {@code CANCELLED}.
 *
 * <p>Layer: adapter/outbound/grpc/inventory
 * <p>Module: order-service
 */
public class InventoryAvailabilityTimeoutException extends InventoryAvailabilityException {

    public InventoryAvailabilityTimeoutException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
