package com.arbitrier.platform.idempotency;

/**
 * Lifecycle states of an idempotent operation tracked by {@link IdempotencyStore}.
 */
public enum IdempotencyStatus {

    /**
     * The operation has been received and is currently being processed.
     * A second delivery with the same key while in this state indicates a duplicate in-flight.
     */
    PENDING,

    /**
     * The operation completed successfully.
     * Subsequent deliveries with the same key are safe to acknowledge and discard.
     */
    PROCESSED,

    /**
     * The operation failed.
     * The retry policy determines whether the same key may be reprocessed.
     */
    FAILED
}
