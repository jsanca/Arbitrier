package com.arbitrier.platform.error;

/**
 * Typed problem codes for persistence adapter failures.
 *
 * <p>Adapters catch infrastructure exceptions (JPA, Spring Data) and rethrow as
 * {@link ApplicationProblemException} with one of these codes so that JPA internals
 * do not leak above the adapter boundary.
 *
 * <p>Layer: platform/error
 * <p>Module: platform
 */
public enum PersistenceProblemCode implements ProblemCode {

    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "Order not found", 404),
    STOCK_RESERVATION_NOT_FOUND("STOCK_RESERVATION_NOT_FOUND", "Stock reservation not found", 404),
    CREDIT_RESERVATION_NOT_FOUND("CREDIT_RESERVATION_NOT_FOUND", "Credit reservation not found", 404),
    SAGA_NOT_FOUND("SAGA_NOT_FOUND", "Saga not found", 404),

    /**
     * A concurrent modification was detected. The caller should reload the aggregate and retry.
     * HTTP 409 Conflict.
     */
    OPTIMISTIC_LOCK_CONFLICT("OPTIMISTIC_LOCK_CONFLICT",
            "Concurrent modification detected — reload and retry", 409),

    /**
     * An unexpected persistence infrastructure failure. HTTP 500 Internal Server Error.
     */
    PERSISTENCE_FAILURE("PERSISTENCE_FAILURE",
            "A persistence operation failed unexpectedly", 500);

    private final String code;
    private final String description;
    private final int httpStatus;

    PersistenceProblemCode(String code, String description, int httpStatus) {
        this.code = code;
        this.description = description;
        this.httpStatus = httpStatus;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }
}
