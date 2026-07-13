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
