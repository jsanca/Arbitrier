package com.arbitrier.order.adapter.outbound.grpc.inventory;

/**
 * Thrown when the Inventory gRPC response violates the expected protocol contract.
 *
 * <p>Covers: {@code AVAILABILITY_STATUS_UNSPECIFIED} from the server, malformed response
 * (e.g. AVAILABLE with non-empty unavailable items), and {@code INVALID_ARGUMENT} responses
 * indicating that the adapter sent an invalid request.
 *
 * <p>Layer: adapter/outbound/grpc/inventory
 * <p>Module: order-service
 */
public class InventoryAvailabilityProtocolException extends InventoryAvailabilityException {

    public InventoryAvailabilityProtocolException(final String message) {
        super(message);
    }

    public InventoryAvailabilityProtocolException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
