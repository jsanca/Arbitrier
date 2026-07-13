package com.arbitrier.platform.messaging.inbox;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and querying inbox events.
 *
 * <p>Consumers check whether an inbound event has already been recorded before processing,
 * and mark it as processed once done.
 *
 * <p>Layer: platform/messaging/inbox
 * <p>Module: platform
 */
public interface InboxRepository {

    /**
     * Persist a new inbox event.
     *
     * @param event the event to save; must not be null
     */
    void save(InboxEvent event);

    /**
     * Look up an event by its identifier.
     *
     * @param eventId the event identifier; must not be null
     * @return the event if present
     */
    Optional<InboxEvent> findById(UUID eventId);

    /**
     * Mark the event with the given identifier as {@link ProcessingStatus#PROCESSED}.
     *
     * @param eventId the event identifier; must not be null
     */
    void markProcessed(UUID eventId);
}
