package com.arbitrier.platform.messaging.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and querying outbox events.
 *
 * <p>Application services save events through this port in the same transaction as the
 * aggregate change. A separate publisher process reads pending events via
 * {@link #findPending()} and marks them as published or failed.
 *
 * <p>Layer: platform/messaging/outbox
 * <p>Module: platform
 */
public interface OutboxRepository {

    /**
     * Persist a new outbox event.
     *
     * @param event the event to save; must not be null
     */
    void save(OutboxEvent event);

    /**
     * Return all events that are still in the {@link PublishStatus#PENDING} state.
     *
     * @return a list of pending events (may be empty, never null)
     */
    List<OutboxEvent> findPending();

    /**
     * Return at most {@code limit} events in the {@link PublishStatus#PENDING} state,
     * ordered by {@link OutboxEvent#occurredAt()} ascending (oldest first).
     *
     * <p>Events with any other status ({@code PUBLISHED}, {@code FAILED}) are excluded.
     * Ordering is deterministic: events with the same {@code occurredAt} value may
     * appear in any stable order for that timestamp.
     *
     * <p>A limit of zero returns an empty list immediately without querying the store.
     * A limit larger than the number of available pending events returns all of them.
     *
     * @param limit maximum number of events to return; must not be negative
     * @return at most {@code limit} pending events, oldest first; never null
     * @throws IllegalArgumentException if {@code limit} is negative
     */
    List<OutboxEvent> findPending(int limit);

    /**
     * Atomically transition a {@link PublishStatus#PENDING} event to
     * {@link PublishStatus#CLAIMED}, recording the claiming worker.
     *
     * <p>The underlying operation is a single conditional UPDATE:
     * {@code WHERE id = eventId AND publish_status = 'PENDING'}. If two workers
     * race on the same event, exactly one will win and the other will receive
     * {@link Optional#empty()} — no exception is thrown for the loser.
     *
     * @param eventId   the event to claim; must not be null
     * @param workerId  identifier of the claiming worker; must not be blank
     * @param claimedAt timestamp to record on the claim; must not be null
     * @return the updated {@link OutboxEvent} in {@code CLAIMED} state, or
     *         {@link Optional#empty()} if the event does not exist or is not
     *         currently in {@code PENDING} status
     */
    Optional<OutboxEvent> claimEvent(UUID eventId, String workerId, Instant claimedAt);

    /**
     * Atomically claim up to {@code limit} PENDING events for the given worker.
     *
     * <p>The implementation must establish ownership before returning events to the caller.
     * The underlying strategy is a {@code SELECT ... FOR UPDATE SKIP LOCKED} (to skip rows
     * held by other concurrent workers) followed by a bulk {@code UPDATE} that transitions
     * all selected rows to {@link PublishStatus#CLAIMED} — both within a single short
     * database transaction that commits before any dispatch occurs.
     *
     * <p>Events are selected in FIFO order: {@code occurredAt ASC}, with {@code eventId ASC}
     * as a deterministic tiebreaker when timestamps collide.
     *
     * <p>Two workers calling this method concurrently will receive disjoint result sets;
     * the database arbitrates ownership via row-level locking.
     *
     * @param workerId  non-blank identifier of the claiming worker
     * @param claimedAt non-null timestamp to record on each claimed row
     * @param limit     maximum number of events to claim; must not be negative
     * @return a list of up to {@code limit} events in {@link PublishStatus#CLAIMED} state,
     *         ordered by {@code occurredAt ASC, eventId ASC}; empty when no PENDING events
     *         exist or {@code limit} is zero; never null
     * @throws IllegalArgumentException if {@code workerId} is blank or {@code limit} is negative
     * @throws NullPointerException     if {@code workerId} or {@code claimedAt} is null
     */
    List<OutboxEvent> claimPending(String workerId, Instant claimedAt, int limit);

    /**
     * Mark the event with the given identifier as {@link PublishStatus#PUBLISHED}.
     *
     * @param eventId the event identifier; must not be null
     */
    void markPublished(UUID eventId);

    /**
     * Mark the event with the given identifier as {@link PublishStatus#FAILED} and record
     * the failed attempt.
     *
     * @param eventId the event identifier; must not be null
     */
    void markFailed(UUID eventId);
}
