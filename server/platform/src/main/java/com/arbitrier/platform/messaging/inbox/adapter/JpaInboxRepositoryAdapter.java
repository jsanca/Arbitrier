package com.arbitrier.platform.messaging.inbox.adapter;

import com.arbitrier.platform.messaging.inbox.InboxEvent;
import com.arbitrier.platform.messaging.inbox.InboxRepository;
import com.arbitrier.platform.messaging.inbox.ProcessingStatus;
import com.arbitrier.platform.time.TimeProvider;
import com.arbitrier.platform.validation.Require;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed {@link InboxRepository} implementation.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: platform
 */
public final class JpaInboxRepositoryAdapter implements InboxRepository {

    private final SpringDataInboxRepository repository;
    private final TimeProvider timeProvider;

    public JpaInboxRepositoryAdapter(final SpringDataInboxRepository repository, final TimeProvider timeProvider) {

        this.repository = Require.notNull(repository, "repository");
        this.timeProvider = Require.notNull(timeProvider, "timeProvider");
    }

    @Override
    public void save(final InboxEvent event) {

        Require.notNull(event, "event");
        repository.save(toEntity(event));
    }

    @Override
    public Optional<InboxEvent> findById(final UUID eventId) {

        Require.notNull(eventId, "eventId");
        return repository.findById(eventId).map(JpaInboxRepositoryAdapter::toRecord);
    }

    @Override
    public void markProcessed(final UUID eventId) {

        Require.notNull(eventId, "eventId");
        final InboxEventEntity entity = repository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No inbox event found with id: " + eventId));
        entity.setProcessingStatus(ProcessingStatus.PROCESSED.name());
        entity.setProcessedAt(timeProvider.now());
        repository.save(entity);
    }

    private static InboxEventEntity toEntity(final InboxEvent event) {

        final InboxEventEntity entity = new InboxEventEntity();
        entity.setId(event.eventId());
        entity.setConsumerId(event.consumerId());
        entity.setReceivedAt(event.receivedAt());
        entity.setProcessedAt(event.processedAt());
        entity.setProcessingStatus(event.processingStatus().name());
        entity.setCorrelationId(event.correlationId());
        entity.setPayloadHash(event.payloadHash());
        return entity;
    }

    private static InboxEvent toRecord(final InboxEventEntity entity) {
        return new InboxEvent(
                entity.getId(),
                entity.getConsumerId(),
                entity.getReceivedAt(),
                entity.getProcessedAt(),
                ProcessingStatus.valueOf(entity.getProcessingStatus()),
                entity.getCorrelationId(),
                entity.getPayloadHash());
    }
}
