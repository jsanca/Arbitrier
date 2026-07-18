package com.arbitrier.inventory.adapter.inbound.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Maps exceptions from the application layer to appropriate gRPC status errors.
 *
 * <p>Business unavailability (insufficient stock, product not found) must never be mapped
 * here — those are successful RPC responses with {@code AVAILABILITY_STATUS_UNAVAILABLE}.
 *
 * <p>Layer: adapter/inbound/grpc
 * <p>Module: inventory-service
 */
public class InventoryGrpcExceptionMapper {

    /**
     * Maps an exception to a gRPC {@link StatusRuntimeException}.
     *
     * <ul>
     *   <li>{@link IllegalArgumentException} → {@code INVALID_ARGUMENT} (client validation failure)</li>
     *   <li>{@link NullPointerException} → {@code INTERNAL} (server programming defect, not a client error)</li>
     *   <li>All other exceptions → {@code INTERNAL}</li>
     * </ul>
     *
     * @param exception the exception to map
     * @return the corresponding gRPC status exception
     */
    public StatusRuntimeException toStatusException(final Throwable exception) {
        if (exception instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .asRuntimeException();
        }
        return Status.INTERNAL
                .withDescription("An unexpected error occurred")
                .asRuntimeException();
    }
}
