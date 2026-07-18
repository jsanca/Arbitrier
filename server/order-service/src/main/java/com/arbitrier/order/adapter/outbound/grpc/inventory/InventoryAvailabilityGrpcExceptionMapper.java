package com.arbitrier.order.adapter.outbound.grpc.inventory;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Maps gRPC {@link StatusRuntimeException} to the Order adapter's technical exception hierarchy.
 *
 * <p>{@code StatusRuntimeException} must not propagate into the Order application or domain layers.
 * This mapper converts each gRPC status code to the appropriate
 * {@link InventoryAvailabilityException} subtype.
 *
 * <p>Layer: adapter/outbound/grpc/inventory
 * <p>Module: order-service
 */
public class InventoryAvailabilityGrpcExceptionMapper {

    /**
     * Maps a gRPC exception to a typed {@link InventoryAvailabilityException}.
     *
     * @param exception the exception thrown by the gRPC stub
     * @return a typed {@code InventoryAvailabilityException} — never null
     */
    public InventoryAvailabilityException toAdapterException(final Exception exception) {
        if (exception instanceof final StatusRuntimeException sre) {
            final Status.Code code = sre.getStatus().getCode();
            return switch (code) {
                case DEADLINE_EXCEEDED, CANCELLED ->
                        new InventoryAvailabilityTimeoutException(
                                "Inventory availability query timed out: " + code, sre);
                case UNAVAILABLE ->
                        new InventoryAvailabilityRemoteUnavailableException(
                                "Inventory Service is currently unavailable", sre);
                case INVALID_ARGUMENT ->
                        new InventoryAvailabilityProtocolException(
                                "Inventory rejected the request as invalid — check adapter mapping", sre);
                default ->
                        new InventoryAvailabilityInternalException(
                                "Inventory availability query failed with gRPC status: " + code, sre);
            };
        }
        return new InventoryAvailabilityInternalException(
                "Unexpected failure during inventory availability query", exception);
    }
}
