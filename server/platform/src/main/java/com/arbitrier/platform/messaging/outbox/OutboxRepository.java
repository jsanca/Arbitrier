package com.arbitrier.platform.messaging.outbox;

import java.util.List;
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
