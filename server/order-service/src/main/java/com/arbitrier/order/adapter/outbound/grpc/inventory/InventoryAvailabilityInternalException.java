package com.arbitrier.order.adapter.outbound.grpc.inventory;

/**
 * Thrown for unexpected gRPC failures from Inventory Service.
 *
 * <p>Maps gRPC status codes: {@code INTERNAL}, {@code UNKNOWN}, {@code DATA_LOSS},
 * and any unrecognised status codes.
 *
 * <p>Layer: adapter/outbound/grpc/inventory
 * <p>Module: order-service
 */
public class InventoryAvailabilityInternalException extends InventoryAvailabilityException {

    public InventoryAvailabilityInternalException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
