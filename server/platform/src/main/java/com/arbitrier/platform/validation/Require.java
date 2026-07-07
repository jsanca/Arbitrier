package com.arbitrier.platform.validation;

import java.util.Collection;

/**
 * Static precondition helpers for validating method arguments.
 *
 * <p>Throws {@link IllegalArgumentException} for blank or invalid values and
 * {@link NullPointerException} (via {@link java.util.Objects#requireNonNull}) for null values.
 * These are programming-contract violations, not business failures — use
 * {@link com.arbitrier.platform.result.Result} for expected business-level failures.
 *
 * <p>Usage:
 * <pre>{@code
 * public record OrderId(String value) {
 *     public OrderId {
 *         Require.notBlank(value, "OrderId.value");
 *     }
 * }
 * }</pre>
 */
public final class Require {

    private Require() {
    }

    /**
     * Asserts that {@code value} is not null.
     *
     * @param value     the value to check
     * @param fieldName name used in the exception message
     * @param <T>       value type
     * @return {@code value} for use in compact record constructors
     * @throws NullPointerException if {@code value} is null
     */
    public static <T> T notNull(T value, String fieldName) {
        if (value == null) {
            throw new NullPointerException(fieldName + " must not be null");
        }
        return value;
    }

    /**
     * Asserts that {@code value} is not null and not blank.
     *
     * @param value     the string to check
     * @param fieldName name used in the exception message
     * @return {@code value} trimmed of no whitespace (original value returned)
     * @throws NullPointerException     if {@code value} is null
     * @throws IllegalArgumentException if {@code value} is blank
     */
    public static String notBlank(String value, String fieldName) {
        notNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    /**
     * Asserts that {@code value} is not null and not empty.
     *
     * @param value     the collection to check
     * @param fieldName name used in the exception message
     * @param <T>       element type
     * @param <C>       collection type
     * @return {@code value}
     * @throws NullPointerException     if {@code value} is null
     * @throws IllegalArgumentException if {@code value} is empty
     */
    public static <T, C extends Collection<T>> C notEmpty(C value, String fieldName) {
        notNull(value, fieldName);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return value;
    }

    /**
     * Asserts that {@code condition} is true.
     *
     * @param condition  the boolean to check
     * @param message    exception message if the condition is false
     * @throws IllegalArgumentException if {@code condition} is false
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
