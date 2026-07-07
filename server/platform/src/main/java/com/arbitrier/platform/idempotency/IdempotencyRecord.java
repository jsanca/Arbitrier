package com.arbitrier.platform.idempotency;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable snapshot of an idempotent operation stored by {@link IdempotencyStore}.
 *
 * <p>{@code processedAt} is {@code null} when the record is in {@link IdempotencyStatus#PENDING}
 * and populated once the operation reaches a terminal state.
 *
 * @param key         the unique key for this operation; never null
 * @param status      the current processing state; never null
 * @param createdAt   when the record was first created; never null
 * @param processedAt when the operation reached a terminal state; null if still pending
 */
public record IdempotencyRecord(
        IdempotencyKey key,
        IdempotencyStatus status,
        Instant createdAt,
        Instant processedAt
) {

    /** Validates required fields; {@code processedAt} is allowed to be null. */
    public IdempotencyRecord {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * Creates a new {@link IdempotencyStatus#PENDING} record.
     *
     * @param key       the idempotency key; must not be null
     * @param createdAt the creation timestamp; must not be null
     * @return a pending record
     */
    public static IdempotencyRecord pending(IdempotencyKey key, Instant createdAt) {
        return new IdempotencyRecord(key, IdempotencyStatus.PENDING, createdAt, null);
    }

    /**
     * Returns a copy of this record in {@link IdempotencyStatus#PROCESSED} state.
     *
     * @param processedAt the completion timestamp; must not be null
     * @return an updated record
     */
    public IdempotencyRecord markProcessed(Instant processedAt) {
        Objects.requireNonNull(processedAt, "processedAt must not be null");
        return new IdempotencyRecord(key, IdempotencyStatus.PROCESSED, createdAt, processedAt);
    }

    /**
     * Returns a copy of this record in {@link IdempotencyStatus#FAILED} state.
     *
     * @param processedAt the failure timestamp; must not be null
     * @return an updated record
     */
    public IdempotencyRecord markFailed(Instant processedAt) {
        Objects.requireNonNull(processedAt, "processedAt must not be null");
        return new IdempotencyRecord(key, IdempotencyStatus.FAILED, createdAt, processedAt);
    }

    /** Returns {@code true} if this record is in a terminal state (processed or failed). */
    public boolean isTerminal() {
        return status == IdempotencyStatus.PROCESSED || status == IdempotencyStatus.FAILED;
    }
}
