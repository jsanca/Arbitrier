package com.arbitrier.platform.correlation;

import com.arbitrier.platform.validation.Require;

import java.util.UUID;

/**
 * Identifies the message that directly caused the current message to be produced.
 *
 * <p>Forms a causal chain: {@code message.causationId = cause.messageId}.
 * Together with {@link CorrelationId} and {@link MessageId}, this enables complete
 * message lineage tracing across event-driven flows.
 */
public record CausationId(String value) {

    /** Validates that the value is not blank. */
    public CausationId {
        Require.notBlank(value, "CausationId.value");
    }

    /**
     * Generates a new random {@code CausationId}.
     *
     * @return a new {@code CausationId} backed by a random UUID
     */
    public static CausationId generate() {
        return new CausationId(UUID.randomUUID().toString());
    }

    /**
     * Wraps an existing string value.
     *
     * @param value the raw string; must not be blank
     * @return a {@code CausationId} wrapping {@code value}
     */
    public static CausationId of(String value) {
        return new CausationId(value);
    }

    /** Returns the raw string value — safe for logging (no PII). */
    @Override
    public String toString() {
        return value;
    }
}
