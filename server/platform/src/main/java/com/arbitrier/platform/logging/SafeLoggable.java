package com.arbitrier.platform.logging;

/**
 * Implemented by types that can produce a PII-safe string representation for log output.
 *
 * <p>Use this on domain objects or value types that might otherwise expose sensitive data
 * (customer names, email addresses, credit limits) when their default {@link #toString()} is logged.
 *
 * <p>Example:
 * <pre>{@code
 * public record CustomerEmail(String value) implements SafeLoggable {
 *     @Override
 *     public String toSafeLogString() {
 *         return "***@" + value.substring(value.indexOf('@') + 1); // mask local-part
 *     }
 * }
 * }</pre>
 */
public interface SafeLoggable {

    /**
     * Returns a representation safe for log output.
     *
     * <p>Must never include PII such as names, email addresses, phone numbers,
     * tax IDs, credit card numbers, or financial limits.
     *
     * @return a non-null, PII-free string
     */
    String toSafeLogString();
}
