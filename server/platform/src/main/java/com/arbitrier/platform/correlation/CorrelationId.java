package com.arbitrier.platform.correlation;

import com.arbitrier.platform.validation.Require;

import java.util.UUID;

/**
 * Identifies a logical request or command across service boundaries.
 *
 * <p>Attach a {@code CorrelationId} to every inbound message and propagate it to all
 * outbound messages in the same logical flow. Store in MDC as
 * {@link com.arbitrier.platform.logging.StructuredLogFields#CORRELATION_ID}.
 */
public record CorrelationId(String value) {

    /** Validates that the value is not blank. */
    public CorrelationId {
        Require.notBlank(value, "CorrelationId.value");
    }

    /**
     * Generates a new random {@code CorrelationId}.
     *
     * @return a new {@code CorrelationId} backed by a random UUID
     */
    public static CorrelationId generate() {
        return new CorrelationId(UUID.randomUUID().toString());
    }

    /**
     * Wraps an existing string value.
     *
     * @param value the raw string; must not be blank
     * @return a {@code CorrelationId} wrapping {@code value}
     */
    public static CorrelationId of(String value) {
        return new CorrelationId(value);
    }

    /** Returns the raw string value — safe for logging (no PII). */
    @Override
    public String toString() {
        return value;
    }
}
