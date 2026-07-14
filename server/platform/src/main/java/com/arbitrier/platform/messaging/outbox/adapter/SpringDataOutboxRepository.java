package com.arbitrier.platform.messaging.outbox.adapter;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link OutboxEventEntity}.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: platform
 */
public interface SpringDataOutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /**
     * Find all outbox rows whose {@code publish_status} matches the given value.
     *
     * @param publishStatus the status string (e.g. {@code "PENDING"})
     * @return matching rows (may be empty, never null)
     */
    List<OutboxEventEntity> findByPublishStatus(String publishStatus);

    /**
     * Find outbox rows by status, ordered by {@code occurred_at} ascending, up to the
     * page size specified by {@code pageable}. Use {@code PageRequest.of(0, limit)} to
     * apply a top-N limit without offset.
     *
     * @param publishStatus the status string (e.g. {@code "PENDING"})
     * @param pageable      page request carrying the limit; offset is always 0
     * @return matching rows in ascending occurrence order (may be empty, never null)
     */
    List<OutboxEventEntity> findByPublishStatusOrderByOccurredAtAsc(String publishStatus, Pageable pageable);
}
