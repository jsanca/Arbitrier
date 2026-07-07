package com.arbitrier.platform.correlation;

import com.arbitrier.platform.validation.Require;

import java.util.UUID;

/**
 * Unique identifier for a single message (event or command) in the system.
 *
 * <p>Every produced message carries a unique {@code MessageId}. Consumers use it for
 * idempotency checks and for constructing {@link CausationId} on replies.
 */
public record MessageId(String value) {

    /** Validates that the value is not blank. */
    public MessageId {
        Require.notBlank(value, "MessageId.value");
    }

    /**
     * Generates a new random {@code MessageId}.
     *
     * @return a new {@code MessageId} backed by a random UUID
     */
    public static MessageId generate() {
        return new MessageId(UUID.randomUUID().toString());
    }

    /**
     * Wraps an existing string value.
     *
     * @param value the raw string; must not be blank
     * @return a {@code MessageId} wrapping {@code value}
     */
    public static MessageId of(String value) {
        return new MessageId(value);
    }

    /** Returns the raw string value — safe for logging (no PII). */
    @Override
    public String toString() {
        return value;
    }
}
