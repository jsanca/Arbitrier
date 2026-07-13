package com.arbitrier.platform.messaging.inbox.adapter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data repository for {@link InboxEventEntity}.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: platform
 */
public interface SpringDataInboxRepository extends JpaRepository<InboxEventEntity, UUID> {
}
