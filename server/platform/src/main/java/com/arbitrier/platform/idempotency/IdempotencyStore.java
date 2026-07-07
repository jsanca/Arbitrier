package com.arbitrier.platform.idempotency;

import java.util.Optional;

/**
 * Outbound port for persisting and querying {@link IdempotencyRecord}s.
 *
 * <p>Implementations are provided by the {@code adapter/outbound/persistence} layer of each
 * service. No persistence implementation exists yet — this interface defines the contract only.
 *
 * <p>Consumers follow the check-then-act pattern:
 * <pre>{@code
 * Optional<IdempotencyRecord> existing = store.find(key);
 * if (existing.isPresent() && existing.get().isTerminal()) {
 *     return; // already processed — discard duplicate
 * }
 * // ... process ...
 * store.save(record.markProcessed(clock.now()));
 * }</pre>
 */
public interface IdempotencyStore {

    /**
     * Finds an existing record for the given key.
     *
     * @param key the idempotency key to look up; must not be null
     * @return an {@link Optional} containing the record, or empty if not found
     */
    Optional<IdempotencyRecord> find(IdempotencyKey key);

    /**
     * Persists a new record.
     *
     * @param record the record to save; must not be null
     */
    void save(IdempotencyRecord record);

    /**
     * Updates an existing record (e.g. transitions from PENDING to PROCESSED or FAILED).
     *
     * @param record the updated record; must not be null
     */
    void update(IdempotencyRecord record);
}
