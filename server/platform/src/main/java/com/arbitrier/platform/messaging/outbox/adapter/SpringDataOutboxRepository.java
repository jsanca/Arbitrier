package com.arbitrier.platform.messaging.outbox.adapter;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    /**
     * Atomically transition a single PENDING row to CLAIMED.
     *
     * <p>The {@code WHERE} clause constrains on both {@code publish_status = 'PENDING'} so
     * that concurrent workers racing on the same event produce exactly one winner. The
     * EntityManager cache is flushed before and cleared after the bulk update.
     *
     * @param eventId   the target event UUID
     * @param workerId  identifier of the claiming worker
     * @param claimedAt timestamp of the claim
     * @return 1 if the row was updated, 0 if the event did not exist or was not PENDING
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE OutboxEventEntity e
            SET e.publishStatus = 'CLAIMED',
                e.claimedBy     = :workerId,
                e.claimedAt     = :claimedAt
            WHERE e.id = :eventId
              AND e.publishStatus = 'PENDING'
            """)
    int claimPending(@Param("eventId") UUID eventId,
                     @Param("workerId") String workerId,
                     @Param("claimedAt") Instant claimedAt);

    /**
     * Select up to {@code limit} PENDING row IDs for batch claiming, skipping any rows
     * already locked by a concurrent transaction.
     *
     * <p>This is a native PostgreSQL query. {@code FOR UPDATE SKIP LOCKED} acquires a
     * row-level exclusive lock on each returned row within the caller's transaction, and
     * skips any rows already held by another transaction. The caller must issue a
     * subsequent {@link #claimByIds} UPDATE within the same transaction to transition the
     * locked rows to CLAIMED before the transaction commits and releases the locks.
     *
     * <p><strong>PostgreSQL dependency:</strong> {@code FOR UPDATE SKIP LOCKED} is a
     * PostgreSQL-specific feature. This query will not work on H2 or other databases
     * that do not support this syntax. The {@link JpaOutboxRepositoryAdapter} uses an
     * in-memory test adapter for non-PostgreSQL test environments.
     *
     * <p>Rows are ordered by {@code occurred_at ASC, id ASC} to preserve FIFO semantics
     * with a deterministic tiebreaker.
     *
     * @param limit maximum number of row IDs to select; must be positive
     * @return up to {@code limit} UUIDs of PENDING rows, locked for update
     */
    @Query(value = """
            SELECT id FROM outbox_events
            WHERE publish_status = 'PENDING'
            ORDER BY occurred_at ASC, id ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> selectPendingIdsForClaim(@Param("limit") int limit);

    /**
     * Bulk-update the given rows to CLAIMED, recording the worker and timestamp.
     *
     * <p>This must be called within the same transaction as {@link #selectPendingIdsForClaim}
     * so that the row-level locks acquired by the SELECT are held until the UPDATE commits.
     *
     * @param ids       the event UUIDs to transition; must not be empty
     * @param workerId  identifier of the claiming worker
     * @param claimedAt timestamp of the claim
     * @return the number of rows updated
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE OutboxEventEntity e
            SET e.publishStatus = 'CLAIMED',
                e.claimedBy     = :workerId,
                e.claimedAt     = :claimedAt
            WHERE e.id IN :ids
              AND e.publishStatus = 'PENDING'
            """)
    int claimByIds(@Param("ids") List<UUID> ids,
                   @Param("workerId") String workerId,
                   @Param("claimedAt") Instant claimedAt);
}
