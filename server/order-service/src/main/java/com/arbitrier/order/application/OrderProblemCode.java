package com.arbitrier.order.application;

import com.arbitrier.platform.error.ProblemCode;

/**
 * Application-level problem codes for order-service.
 *
 * <p>Each code declares its own {@link #httpStatus()} so the platform exception handler
 * automatically maps it to the correct HTTP response without service-specific handler logic.
 *
 * <p>Layer: application
 * <p>Module: order-service
 */
public enum OrderProblemCode implements ProblemCode {

    /**
     * The authenticated user does not have permission to submit orders for the given customer.
     * Maps to {@code 403 Forbidden}.
     */
    CUSTOMER_ACCESS_DENIED("ORDER_ACCESS_DENIED", "The user is not authorised to submit orders for this customer", 403),

    /**
     * One or more order lines do not have sufficient inventory at the time of submission.
     * Maps to {@code 422 Unprocessable Entity}.
     */
    ORDER_ITEMS_UNAVAILABLE("ORDER_ITEMS_UNAVAILABLE",
            "One or more order lines do not have sufficient inventory", 422);

    private final String code;
    private final String description;
    private final int httpStatus;

    OrderProblemCode(String code, String description, int httpStatus) {
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
