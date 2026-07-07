package com.arbitrier.platform.idempotency;

import com.arbitrier.platform.validation.Require;

import java.util.UUID;

/**
 * Opaque key that uniquely identifies an operation for idempotency purposes.
 *
 * <p>Consumers generate or extract an {@code IdempotencyKey} from an inbound message
 * (e.g. from a Kafka header or an HTTP {@code Idempotency-Key} header) and use
 * {@link IdempotencyStore} to detect duplicate deliveries.
 */
public record IdempotencyKey(String value) {

    /** Validates that the value is not blank. */
    public IdempotencyKey {
        Require.notBlank(value, "IdempotencyKey.value");
    }

    /**
     * Generates a new random {@code IdempotencyKey}.
     *
     * @return a new key backed by a random UUID
     */
    public static IdempotencyKey generate() {
        return new IdempotencyKey(UUID.randomUUID().toString());
    }

    /**
     * Wraps an existing string value.
     *
     * @param value the raw key string; must not be blank
     * @return an {@code IdempotencyKey} wrapping {@code value}
     */
    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }

    /** Returns the raw key value — safe for logging (no PII). */
    @Override
    public String toString() {
        return value;
    }
}
