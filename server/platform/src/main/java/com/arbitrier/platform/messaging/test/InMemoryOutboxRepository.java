package com.arbitrier.platform.messaging.test;

import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import com.arbitrier.platform.validation.Require;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * In-memory {@link OutboxRepository} for use in unit and integration tests.
 *
 * <p>Not thread-safe; intended for single-threaded test scenarios.
 *
 * <p>Layer: platform/messaging/test
 * <p>Module: platform
 */
public final class InMemoryOutboxRepository implements OutboxRepository {

    private final List<OutboxEvent> events = new ArrayList<>();

    @Override
    public void save(OutboxEvent event) {
        Require.notNull(event, "event");
        events.removeIf(e -> e.eventId().equals(event.eventId()));
        events.add(event);
    }

    @Override
    public List<OutboxEvent> findPending() {
        return events.stream()
                .filter(e -> e.publishStatus() == PublishStatus.PENDING)
                .toList();
    }

    @Override
    public List<OutboxEvent> findPending(final int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative: " + limit);
        }
        return events.stream()
                .filter(e -> e.publishStatus() == PublishStatus.PENDING)
                .sorted(Comparator.comparing(OutboxEvent::occurredAt))
                .limit(limit)
                .toList();
    }

    @Override
    public void markPublished(UUID eventId) {
        Require.notNull(eventId, "eventId");
        replace(eventId, e -> new OutboxEvent(
                e.eventId(), e.aggregateId(), e.aggregateType(), e.eventType(),
                e.payload(), e.payloadFormat(), e.occurredAt(),
                Instant.now(),
                PublishStatus.PUBLISHED,
                e.attemptCount(), e.lastAttempt(), e.correlationId(), e.causationId(),
                e.messageNature()));
    }

    @Override
    public void markFailed(UUID eventId) {
        Require.notNull(eventId, "eventId");
        replace(eventId, e -> new OutboxEvent(
                e.eventId(), e.aggregateId(), e.aggregateType(), e.eventType(),
                e.payload(), e.payloadFormat(), e.occurredAt(),
                e.publishedAt(),
                PublishStatus.FAILED,
                e.attemptCount() + 1,
                Instant.now(),
                e.correlationId(), e.causationId(),
                e.messageNature()));
    }

    /**
     * Return all outbox events currently stored, regardless of publish status.
     * Intended for test assertions.
     */
    public List<OutboxEvent> findAll() {
        return List.copyOf(events);
    }

    /** Clear all stored events. */
    public void clear() {
        events.clear();
    }

    private void replace(UUID eventId, java.util.function.Function<OutboxEvent, OutboxEvent> mutator) {
        for (int i = 0; i < events.size(); i++) {
            OutboxEvent current = events.get(i);
            if (current.eventId().equals(eventId)) {
                events.set(i, mutator.apply(current));
                return;
            }
        }
        throw new IllegalArgumentException("No outbox event found with id: " + eventId);
    }
}
