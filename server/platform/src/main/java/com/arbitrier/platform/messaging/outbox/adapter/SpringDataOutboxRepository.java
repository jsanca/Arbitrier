package com.arbitrier.platform.messaging.outbox.adapter;

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
}
