package com.arbitrier.platform.messaging.test;

import com.arbitrier.platform.messaging.inbox.InboxEvent;
import com.arbitrier.platform.messaging.inbox.InboxRepository;
import com.arbitrier.platform.messaging.inbox.ProcessingStatus;
import com.arbitrier.platform.validation.Require;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory {@link InboxRepository} for use in unit and integration tests.
 *
 * <p>Not thread-safe; intended for single-threaded test scenarios.
 *
 * <p>Layer: platform/messaging/test
 * <p>Module: platform
 */
public final class InMemoryInboxRepository implements InboxRepository {

    private final List<InboxEvent> events = new ArrayList<>();

    @Override
    public void save(InboxEvent event) {
        Require.notNull(event, "event");
        events.removeIf(e -> e.eventId().equals(event.eventId()));
        events.add(event);
    }

    @Override
    public Optional<InboxEvent> findById(UUID eventId) {
        Require.notNull(eventId, "eventId");
        return events.stream().filter(e -> e.eventId().equals(eventId)).findFirst();
    }

    @Override
    public void markProcessed(UUID eventId) {
        Require.notNull(eventId, "eventId");
        for (int i = 0; i < events.size(); i++) {
            InboxEvent current = events.get(i);
            if (current.eventId().equals(eventId)) {
                events.set(i, new InboxEvent(
                        current.eventId(),
                        current.consumerId(),
                        current.receivedAt(),
                        Instant.now(),
                        ProcessingStatus.PROCESSED,
                        current.correlationId(),
                        current.payloadHash()));
                return;
            }
        }
        throw new IllegalArgumentException("No inbox event found with id: " + eventId);
    }

    /** Return all inbox events currently stored. */
    public List<InboxEvent> findAll() {
        return List.copyOf(events);
    }

    /** Clear all stored events. */
    public void clear() {
        events.clear();
    }
}
