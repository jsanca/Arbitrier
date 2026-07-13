package com.arbitrier.platform.messaging.outbox.adapter;

import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import com.arbitrier.platform.time.TimeProvider;
import com.arbitrier.platform.validation.Require;

import java.util.List;
import java.util.UUID;

/**
 * JPA-backed {@link OutboxRepository} implementation.
 *
 * <p>Maps between the domain {@link OutboxEvent} record and the JPA
 * {@link OutboxEventEntity}. Timestamps that are recorded here (publishedAt, lastAttempt)
 * are sourced from the injected {@link TimeProvider}.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: platform
 */
public final class JpaOutboxRepositoryAdapter implements OutboxRepository {

    private final SpringDataOutboxRepository repository;
    private final TimeProvider timeProvider;

    public JpaOutboxRepositoryAdapter(SpringDataOutboxRepository repository, TimeProvider timeProvider) {
        this.repository = Require.notNull(repository, "repository");
        this.timeProvider = Require.notNull(timeProvider, "timeProvider");
    }

    @Override
    public void save(OutboxEvent event) {
        Require.notNull(event, "event");
        repository.save(toEntity(event));
    }

    @Override
    public List<OutboxEvent> findPending() {
        return repository.findByPublishStatus(PublishStatus.PENDING.name()).stream()
                .map(JpaOutboxRepositoryAdapter::toRecord)
                .toList();
    }

    @Override
    public void markPublished(UUID eventId) {
        Require.notNull(eventId, "eventId");
        OutboxEventEntity entity = repository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No outbox event found with id: " + eventId));
        entity.setPublishStatus(PublishStatus.PUBLISHED.name());
        entity.setPublishedAt(timeProvider.now());
        repository.save(entity);
    }

    @Override
    public void markFailed(UUID eventId) {
        Require.notNull(eventId, "eventId");
        OutboxEventEntity entity = repository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No outbox event found with id: " + eventId));
        entity.setPublishStatus(PublishStatus.FAILED.name());
        entity.setAttemptCount(entity.getAttemptCount() + 1);
        entity.setLastAttempt(timeProvider.now());
        repository.save(entity);
    }

    private static OutboxEventEntity toEntity(OutboxEvent event) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setId(event.eventId());
        entity.setAggregateId(event.aggregateId());
        entity.setAggregateType(event.aggregateType());
        entity.setEventType(event.eventType());
        entity.setPayload(event.payload());
        entity.setPayloadFormat(event.payloadFormat());
        entity.setOccurredAt(event.occurredAt());
        entity.setPublishedAt(event.publishedAt());
        entity.setPublishStatus(event.publishStatus().name());
        entity.setAttemptCount(event.attemptCount());
        entity.setLastAttempt(event.lastAttempt());
        entity.setCorrelationId(event.correlationId());
        entity.setCausationId(event.causationId());
        entity.setMessageNature(event.messageNature().name());
        return entity;
    }

    private static OutboxEvent toRecord(OutboxEventEntity entity) {
        return new OutboxEvent(
                entity.getId(),
                entity.getAggregateId(),
                entity.getAggregateType(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getPayloadFormat(),
                entity.getOccurredAt(),
                entity.getPublishedAt(),
                PublishStatus.valueOf(entity.getPublishStatus()),
                entity.getAttemptCount(),
                entity.getLastAttempt(),
                entity.getCorrelationId(),
                entity.getCausationId(),
                MessageNature.valueOf(entity.getMessageNature()));
    }
}
