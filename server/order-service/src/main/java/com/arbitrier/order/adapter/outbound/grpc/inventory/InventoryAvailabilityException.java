package com.arbitrier.order.adapter.outbound.grpc.inventory;

/**
 * Base exception for all technical failures when querying Inventory availability over gRPC.
 *
 * <p>Subtypes distinguish: timeout, remote unavailable, protocol violation, and internal error.
 * Business unavailability ({@link com.arbitrier.order.application.port.outbound.AvailabilityLineResponse}
 * with zero quantity) is not an exception; only communication or protocol failures are.
 *
 * <p>Layer: adapter/outbound/grpc/inventory
 * <p>Module: order-service
 */
public class InventoryAvailabilityException extends RuntimeException {

    public InventoryAvailabilityException(final String message) {
        super(message);
    }

    public InventoryAvailabilityException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
