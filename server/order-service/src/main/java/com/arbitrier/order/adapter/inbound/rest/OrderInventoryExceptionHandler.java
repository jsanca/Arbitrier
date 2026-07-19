package com.arbitrier.order.adapter.inbound.rest;

import com.arbitrier.order.adapter.outbound.grpc.inventory.InventoryAvailabilityException;
import com.arbitrier.order.adapter.outbound.grpc.inventory.InventoryAvailabilityInternalException;
import com.arbitrier.order.adapter.outbound.grpc.inventory.InventoryAvailabilityProtocolException;
import com.arbitrier.order.adapter.outbound.grpc.inventory.InventoryAvailabilityRemoteUnavailableException;
import com.arbitrier.order.adapter.outbound.grpc.inventory.InventoryAvailabilityTimeoutException;
import com.arbitrier.platform.web.ProblemResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles technical {@link InventoryAvailabilityException} subtypes thrown when the
 * inventory gRPC call fails. Maps each subtype to an appropriate HTTP status code.
 *
 * <p>These are transport/integration failures distinct from business unavailability
 * ({@code ORDER_ITEMS_UNAVAILABLE}), which is handled via
 * {@link com.arbitrier.platform.web.PlatformExceptionHandler}.
 *
 * <p>Layer: adapter/inbound/rest
 * <p>Module: order-service
 */
@RestControllerAdvice
public class OrderInventoryExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderInventoryExceptionHandler.class);

    /** Inventory availability deadline exceeded → {@code 504 Gateway Timeout}. */
    @ExceptionHandler(InventoryAvailabilityTimeoutException.class)
    public ResponseEntity<ProblemResponse> handleInventoryTimeout(
            final InventoryAvailabilityTimeoutException ex) {
        log.warn("Inventory availability timed out: {}", ex.getMessage());
        return ResponseEntity.status(504)
                .body(ProblemResponse.of("INVENTORY_TIMEOUT",
                        "Inventory availability check timed out", 504));
    }

    /** Inventory service unreachable → {@code 503 Service Unavailable}. */
    @ExceptionHandler(InventoryAvailabilityRemoteUnavailableException.class)
    public ResponseEntity<ProblemResponse> handleInventoryRemoteUnavailable(
            final InventoryAvailabilityRemoteUnavailableException ex) {
        log.warn("Inventory service unavailable: {}", ex.getMessage());
        return ResponseEntity.status(503)
                .body(ProblemResponse.of("INVENTORY_SERVICE_UNAVAILABLE",
                        "Inventory service is temporarily unavailable", 503));
    }

    /** Protocol violation (malformed response, bad contract) → {@code 502 Bad Gateway}. */
    @ExceptionHandler(InventoryAvailabilityProtocolException.class)
    public ResponseEntity<ProblemResponse> handleInventoryProtocolError(
            final InventoryAvailabilityProtocolException ex) {
        log.error("Inventory availability protocol violation: {}", ex.getMessage(), ex);
        return ResponseEntity.status(502)
                .body(ProblemResponse.of("INVENTORY_PROTOCOL_ERROR",
                        "Inventory service returned an invalid response", 502));
    }

    /** Internal inventory error → {@code 500 Internal Server Error}. */
    @ExceptionHandler(InventoryAvailabilityInternalException.class)
    public ResponseEntity<ProblemResponse> handleInventoryInternalError(
            final InventoryAvailabilityInternalException ex) {
        log.error("Inventory availability internal failure: {}", ex.getMessage(), ex);
        return ResponseEntity.status(500)
                .body(ProblemResponse.of("INVENTORY_INTEGRATION_ERROR",
                        "Inventory service integration error", 500));
    }
}
