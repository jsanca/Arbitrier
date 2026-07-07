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
 *     ORDER_NOT_FOUND("ORDER_001", "Order not found");
 *     // ...
 * }
 * }</pre>
 */
public interface ProblemCode {

    /** Machine-readable code; must be unique within its namespace, e.g. {@code "PLATFORM_NULL_ARGUMENT"}. */
    String code();

    /** Human-readable description of what went wrong. */
    String description();
}
