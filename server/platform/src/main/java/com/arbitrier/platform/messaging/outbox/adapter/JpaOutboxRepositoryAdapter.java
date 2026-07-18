package com.arbitrier.platform.messaging.outbox.adapter;

import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import com.arbitrier.platform.time.TimeProvider;
import com.arbitrier.platform.validation.Require;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed {@link OutboxRepository} implementation.
 *
 * <p>Maps between the domain {@link OutboxEvent} record and the JPA
 * {@link OutboxEventEntity}. Timestamps that are recorded here (publishedAt, lastAttempt)
 * are sourced from the injected {@link TimeProvider}.
 *
 * <p>This class is <strong>not</strong> declared {@code final} because Spring's
 * {@link Transactional @Transactional} support requires CGLIB subclassing proxies.
 * Do not add {@code final} to this class.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: platform
 */
public class JpaOutboxRepositoryAdapter implements OutboxRepository {

    private final SpringDataOutboxRepository repository;
    private final TimeProvider timeProvider;

    public JpaOutboxRepositoryAdapter(final SpringDataOutboxRepository repository, final TimeProvider timeProvider) {
        this.repository = Require.notNull(repository, "repository");
        this.timeProvider = Require.notNull(timeProvider, "timeProvider");
    }

    @Override
    public void save(final OutboxEvent event) {
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
    public List<OutboxEvent> findPending(final int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative: " + limit);
        }
        if (limit == 0) {
            return List.of();
        }
        return repository.findByPublishStatusOrderByOccurredAtAsc(
                        PublishStatus.PENDING.name(), PageRequest.of(0, limit))
                .stream()
                .map(JpaOutboxRepositoryAdapter::toRecord)
                .toList();
    }

    @Transactional
    @Override
    public Optional<OutboxEvent> claimEvent(final UUID eventId, final String workerId, final Instant claimedAt) {
        Require.notNull(eventId, "eventId");
        Require.notBlank(workerId, "workerId");
        Require.notNull(claimedAt, "claimedAt");
        int affected = repository.claimPending(eventId, workerId, claimedAt);
        if (affected == 0) {
            return Optional.empty();
        }
        return repository.findById(eventId).map(JpaOutboxRepositoryAdapter::toRecord);
    }

    /**
     * Atomically claim up to {@code limit} PENDING events using
     * {@code SELECT ... FOR UPDATE SKIP LOCKED} followed by a bulk UPDATE,
     * both within a single short database transaction.
     *
     * <p>The transaction commits before returning to the caller; no locks are held
     * during dispatch. Concurrent workers receive disjoint result sets because
     * {@code SKIP LOCKED} skips rows already held by another in-flight transaction.
     */
    @Transactional
    @Override
    public List<OutboxEvent> claimPending(final String workerId, final Instant claimedAt, final int limit) {

        /*
         * #revision-advise [Performance Optimization & Infrastructure Scalability]
         * Current Architecture Trade-off:
         * This batch-claiming process is highly resilient thanks to PostgreSQL 'FOR UPDATE SKIP LOCKED',
         * preventing lock contention across concurrent workers. However, it requires 3 distinct
         * database roundtrips per batch inside a single transaction:
         *   1. SELECT (Ids for claim)
         *   2. UPDATE (Bulk status transition to CLAIMED)
         *   3. SELECT (findAllById to reconstruct hydrated domain entities since Hibernate cache is cleared)
         *
         * Future Scalability Considerations (If RDBMS CPU/IOPS under heavy load becomes a bottleneck):
         *
         * Option A (Application Level - Low Overhead Modification):
         * Change 'repository.claimByIds' to use PostgreSQL's native 'UPDATE ... RETURNING *' statement.
         * This will atomically update the rows AND return the fully hydrated entity data in a single
         * roundtrip, completely eliminating the need for the subsequent 'repository.findAllById(ids)' call.
         * Note: Requires custom native query mapping to bypass standard Spring Data JPA bulk limitations.
         *
         * Option B (Infrastructure Level - Pure Architectural Change):
         * Migrate entirely to a log-based Change Data Capture (CDC) pipeline using Apache Kafka Connect
         * and Debezium. Under a CDC approach, this worker polling mechanism, state-tracking logic
         * ('PENDING'/'CLAIMED'), and multi-query batch transaction can be fully deprecated.
         * The application would only perform a lightweight INSERT into the outbox table, and Debezium
         * will stream those records asynchronously directly from the database WAL logs straight into Kafka,
         * reducing relational database CPU overhead to near-zero.
         */
        Require.notBlank(workerId, "workerId");
        Require.notNull(claimedAt, "claimedAt");

        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative: " + limit);
        }

        if (limit == 0) {
            return List.of();
        }

        final List<UUID> ids = repository.selectPendingIdsForClaim(limit);
        if (ids.isEmpty()) {
            return List.of();
        }

        repository.claimByIds(ids, workerId, claimedAt);
        return repository.findAllById(ids).stream()
                .map(JpaOutboxRepositoryAdapter::toRecord)
                .sorted(Comparator.comparing(OutboxEvent::occurredAt).thenComparing(OutboxEvent::eventId))
                .toList();
    }

    @Override
    public void markPublished(final UUID eventId) {

        Require.notNull(eventId, "eventId");
        final OutboxEventEntity entity = repository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No outbox event found with id: " + eventId));
        entity.setPublishStatus(PublishStatus.PUBLISHED.name());
        entity.setPublishedAt(timeProvider.now());
        entity.setClaimedBy(null);
        entity.setClaimedAt(null);
        repository.save(entity);
    }

    @Override
    public void markFailed(final UUID eventId) {

        Require.notNull(eventId, "eventId");
        final OutboxEventEntity entity = repository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No outbox event found with id: " + eventId));
        entity.setPublishStatus(PublishStatus.FAILED.name());
        entity.setAttemptCount(entity.getAttemptCount() + 1);
        entity.setLastAttempt(timeProvider.now());
        entity.setClaimedBy(null);
        entity.setClaimedAt(null);
        repository.save(entity);
    }

    private static OutboxEventEntity toEntity(final OutboxEvent event) {

        final OutboxEventEntity entity = new OutboxEventEntity();
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
        entity.setClaimedBy(event.claimedBy());
        entity.setClaimedAt(event.claimedAt());
        return entity;
    }

    private static OutboxEvent toRecord(final OutboxEventEntity entity) {
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
                MessageNature.valueOf(entity.getMessageNature()),
                entity.getClaimedBy(),
                entity.getClaimedAt());
    }
}
