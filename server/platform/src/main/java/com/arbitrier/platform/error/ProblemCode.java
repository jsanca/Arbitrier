package com.arbitrier.platform.error;

/**
 * Contract for a typed, machine-readable error code.
 *
 * <p>Bounded contexts define their own {@code enum} implementing this interface so error codes
 * are namespaced and strongly typed rather than free-form strings.
 *
 * <p>Example:
 * <pre>{@code
 * public enum OrderProblemCode implements ProblemCode {
 *     CUSTOMER_ACCESS_DENIED("ORDER_ACCESS_DENIED", "Customer access denied", 403);
 *     // ...
 * }
 * }</pre>
 */
public interface ProblemCode {

    /** Machine-readable code; must be unique within its namespace, e.g. {@code "PLATFORM_NULL_ARGUMENT"}. */
    String code();

    /** Human-readable description of what went wrong. */
    String description();

    /**
     * The HTTP status code this problem maps to when thrown as an {@link ApplicationProblemException}.
     *
     * <p>Defaults to {@code 422 Unprocessable Entity} for business rule violations.
     * Override for authorization failures ({@code 403}), not-found ({@code 404}), etc.
     */
    default int httpStatus() {
        return 422;
    }
}
