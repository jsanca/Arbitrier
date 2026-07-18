package com.arbitrier.platform.messaging.test;

import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class InMemoryOutboxRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private InMemoryOutboxRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOutboxRepository();
    }

    @Test
    void finds_pending_events() {
        OutboxEvent pending = createEvent(PublishStatus.PENDING);
        OutboxEvent published = createEvent(PublishStatus.PUBLISHED);
        repository.save(pending);
        repository.save(published);

        assertThat(repository.findPending()).containsExactly(pending);
    }

    @Test
    void marks_event_as_published() {
        OutboxEvent event = createEvent(PublishStatus.PENDING);
        repository.save(event);
        repository.markPublished(event.eventId());

        List<OutboxEvent> all = repository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.getFirst().publishStatus()).isEqualTo(PublishStatus.PUBLISHED);
        assertThat(all.getFirst().publishedAt()).isNotNull();
    }

    @Test
    void marks_event_as_failed_and_increments_attempt() {
        OutboxEvent event = createEvent(PublishStatus.PENDING);
        repository.save(event);
        repository.markFailed(event.eventId());

        List<OutboxEvent> all = repository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.getFirst().publishStatus()).isEqualTo(PublishStatus.FAILED);
        assertThat(all.getFirst().attemptCount()).isEqualTo(1);
        assertThat(all.getFirst().lastAttempt()).isNotNull();
    }

    @Test
    void markPublished_throws_for_unknown_event() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> repository.markPublished(UUID.randomUUID()))
                .withMessageContaining("No outbox event found");
    }

    @Test
    void markFailed_throws_for_unknown_event() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> repository.markFailed(UUID.randomUUID()))
                .withMessageContaining("No outbox event found");
    }

    @Test
    void replace_removes_previous_version_on_save() {
        UUID eventId = UUID.randomUUID();
        OutboxEvent v1 = new OutboxEvent(
                eventId, "agg-1", "Order", "Event", "v1", "JSON",
                NOW, null, PublishStatus.PENDING, 0, null, null, null,
                MessageNature.EVENT, null, null);
        repository.save(v1);

        OutboxEvent v2 = new OutboxEvent(
                eventId, "agg-1", "Order", "Event", "v2", "JSON",
                NOW, null, PublishStatus.PUBLISHED, 1, NOW, null, null,
                MessageNature.EVENT, null, null);
        repository.save(v2);

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().getFirst().payload()).isEqualTo("v2");
    }

    @Test
    void clear_removes_all_events() {
        repository.save(createEvent(PublishStatus.PENDING));
        repository.save(createEvent(PublishStatus.PUBLISHED));
        repository.clear();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void findAll_returns_copy_not_live_view() {
        repository.save(createEvent(PublishStatus.PENDING));
        List<OutboxEvent> snapshot = repository.findAll();

        repository.clear();
        assertThat(snapshot).hasSize(1);
    }

    @Test
    void findPending_with_limit_returns_empty_when_no_pending() {
        repository.save(createEvent(PublishStatus.PUBLISHED));

        assertThat(repository.findPending(10)).isEmpty();
    }

    @Test
    void findPending_with_limit_returns_oldest_first() {
        OutboxEvent newest = pendingAt(NOW.plusSeconds(2));
        OutboxEvent oldest = pendingAt(NOW);
        OutboxEvent middle = pendingAt(NOW.plusSeconds(1));
        repository.save(newest);
        repository.save(oldest);
        repository.save(middle);

        List<OutboxEvent> result = repository.findPending(2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).eventId()).isEqualTo(oldest.eventId());
        assertThat(result.get(1).eventId()).isEqualTo(middle.eventId());
    }

    @Test
    void findPending_with_limit_larger_than_available_returns_all() {
        repository.save(pendingAt(NOW));
        repository.save(pendingAt(NOW.plusSeconds(1)));

        assertThat(repository.findPending(100)).hasSize(2);
    }

    @Test
    void findPending_with_limit_0_returns_empty() {
        repository.save(createEvent(PublishStatus.PENDING));

        assertThat(repository.findPending(0)).isEmpty();
    }

    @Test
    void findPending_with_negative_limit_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> repository.findPending(-1))
                .withMessageContaining("negative");
    }

    @Test
    void findPending_with_limit_excludes_non_pending() {
        repository.save(pendingAt(NOW));
        OutboxEvent published = pendingAt(NOW.plusSeconds(1));
        repository.save(published);
        repository.markPublished(published.eventId());
        OutboxEvent failed = pendingAt(NOW.plusSeconds(2));
        repository.save(failed);
        repository.markFailed(failed.eventId());

        assertThat(repository.findPending(10)).hasSize(1);
    }

    // ── claimEvent ────────────────────────────────────────────────────────────

    @Test
    void claimEvent_succeeds_on_pending_event() {
        OutboxEvent pending = pendingAt(NOW);
        repository.save(pending);

        Optional<OutboxEvent> result = repository.claimEvent(pending.eventId(), "worker-1", NOW.plusSeconds(1));

        assertThat(result).isPresent();
        assertThat(result.get().publishStatus()).isEqualTo(PublishStatus.CLAIMED);
        assertThat(result.get().claimedBy()).isEqualTo("worker-1");
        assertThat(result.get().claimedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void claimEvent_returns_empty_if_already_claimed() {
        OutboxEvent claimed = pendingAt(NOW).claim("worker-1", NOW.plusSeconds(1));
        repository.save(claimed);

        Optional<OutboxEvent> result = repository.claimEvent(claimed.eventId(), "worker-2", NOW.plusSeconds(2));

        assertThat(result).isEmpty();
    }

    @Test
    void claimEvent_returns_empty_if_published() {
        OutboxEvent pending = pendingAt(NOW);
        repository.save(pending);
        repository.markPublished(pending.eventId());

        Optional<OutboxEvent> result = repository.claimEvent(pending.eventId(), "worker-1", NOW.plusSeconds(1));

        assertThat(result).isEmpty();
    }

    @Test
    void claimEvent_returns_empty_if_failed() {
        OutboxEvent pending = pendingAt(NOW);
        repository.save(pending);
        repository.markFailed(pending.eventId());

        Optional<OutboxEvent> result = repository.claimEvent(pending.eventId(), "worker-1", NOW.plusSeconds(1));

        assertThat(result).isEmpty();
    }

    @Test
    void claimEvent_returns_empty_if_not_found() {
        Optional<OutboxEvent> result = repository.claimEvent(UUID.randomUUID(), "worker-1", NOW);

        assertThat(result).isEmpty();
    }

    @Test
    void claimEvent_concurrent_exactly_one_winner() throws InterruptedException {
        OutboxEvent pending = pendingAt(NOW);
        repository.save(pending);

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch done = new CountDownLatch(2);

        for (int i = 0; i < 2; i++) {
            String workerId = "worker-" + i;
            new Thread(() -> {
                ready.countDown();
                try { ready.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                Optional<OutboxEvent> result = repository.claimEvent(pending.eventId(), workerId, NOW.plusSeconds(1));
                if (result.isPresent()) successCount.incrementAndGet();
                done.countDown();
            }).start();
        }

        done.await();
        assertThat(successCount.get()).isEqualTo(1);
    }

    @Test
    void findPending_excludes_claimed() {
        OutboxEvent pending = pendingAt(NOW);
        repository.save(pending);

        OutboxEvent claimed = pendingAt(NOW.plusSeconds(1)).claim("worker-1", NOW.plusSeconds(2));
        repository.save(claimed);

        assertThat(repository.findPending()).containsExactly(pending);
    }

    @Test
    void findPending_with_limit_excludes_claimed() {
        OutboxEvent pending = pendingAt(NOW);
        repository.save(pending);

        OutboxEvent claimed = pendingAt(NOW.plusSeconds(1)).claim("worker-1", NOW.plusSeconds(2));
        repository.save(claimed);

        List<OutboxEvent> result = repository.findPending(10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().eventId()).isEqualTo(pending.eventId());
    }

    // ── claimPending (batch) ─────────────────────────────────────────────────

    @Test
    void claimPending_returns_empty_when_no_pending_events() {
        repository.save(pendingAt(NOW));
        repository.markPublished(repository.findPending().getFirst().eventId());

        assertThat(repository.claimPending("worker-1", NOW, 10)).isEmpty();
    }

    @Test
    void claimPending_claims_single_pending_event() {
        repository.save(pendingAt(NOW));

        List<OutboxEvent> result = repository.claimPending("worker-1", NOW.plusSeconds(1), 10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().publishStatus()).isEqualTo(PublishStatus.CLAIMED);
        assertThat(result.getFirst().claimedBy()).isEqualTo("worker-1");
        assertThat(result.getFirst().claimedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void claimPending_preserves_fifo_order() {
        OutboxEvent oldest = pendingAt(NOW);
        OutboxEvent middle = pendingAt(NOW.plusSeconds(1));
        OutboxEvent newest = pendingAt(NOW.plusSeconds(2));
        repository.save(newest);
        repository.save(oldest);
        repository.save(middle);

        List<OutboxEvent> result = repository.claimPending("worker-1", NOW.plusSeconds(5), 10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).eventId()).isEqualTo(oldest.eventId());
        assertThat(result.get(1).eventId()).isEqualTo(middle.eventId());
        assertThat(result.get(2).eventId()).isEqualTo(newest.eventId());
    }

    @Test
    void claimPending_respects_limit() {
        repository.save(pendingAt(NOW));
        repository.save(pendingAt(NOW.plusSeconds(1)));
        repository.save(pendingAt(NOW.plusSeconds(2)));

        List<OutboxEvent> result = repository.claimPending("worker-1", NOW.plusSeconds(5), 2);

        assertThat(result).hasSize(2);
    }

    @Test
    void claimPending_limit_zero_returns_empty() {
        repository.save(pendingAt(NOW));

        assertThat(repository.claimPending("worker-1", NOW, 0)).isEmpty();
    }

    @Test
    void claimPending_negative_limit_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> repository.claimPending("worker-1", NOW, -1))
                .withMessageContaining("negative");
    }

    @Test
    void claimPending_blank_workerId_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> repository.claimPending("  ", NOW, 10))
                .withMessageContaining("workerId");
    }

    @Test
    void claimPending_null_claimedAt_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> repository.claimPending("worker-1", null, 10))
                .withMessageContaining("claimedAt");
    }

    @Test
    void claimPending_excludes_published_events() {
        OutboxEvent pending = pendingAt(NOW);
        repository.save(pending);
        OutboxEvent published = pendingAt(NOW.plusSeconds(1));
        repository.save(published);
        repository.markPublished(published.eventId());

        List<OutboxEvent> result = repository.claimPending("worker-1", NOW.plusSeconds(5), 10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().eventId()).isEqualTo(pending.eventId());
    }

    @Test
    void claimPending_excludes_failed_events() {
        OutboxEvent pending = pendingAt(NOW);
        repository.save(pending);
        OutboxEvent failed = pendingAt(NOW.plusSeconds(1));
        repository.save(failed);
        repository.markFailed(failed.eventId());

        List<OutboxEvent> result = repository.claimPending("worker-1", NOW.plusSeconds(5), 10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().eventId()).isEqualTo(pending.eventId());
    }

    @Test
    void claimPending_excludes_already_claimed_events() {
        OutboxEvent pending = pendingAt(NOW);
        repository.save(pending);
        OutboxEvent alreadyClaimed = pendingAt(NOW.plusSeconds(1)).claim("other-worker", NOW.plusSeconds(1));
        repository.save(alreadyClaimed);

        List<OutboxEvent> result = repository.claimPending("worker-1", NOW.plusSeconds(5), 10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().eventId()).isEqualTo(pending.eventId());
    }

    @Test
    void claimPending_persists_claim_metadata_on_all_claimed_events() {
        repository.save(pendingAt(NOW));
        repository.save(pendingAt(NOW.plusSeconds(1)));

        List<OutboxEvent> result = repository.claimPending("worker-batch", NOW.plusSeconds(5), 10);

        assertThat(result).hasSize(2);
        result.forEach(e -> {
            assertThat(e.claimedBy()).isEqualTo("worker-batch");
            assertThat(e.claimedAt()).isEqualTo(NOW.plusSeconds(5));
            assertThat(e.publishStatus()).isEqualTo(PublishStatus.CLAIMED);
        });
    }

    @Test
    void claimPending_total_claimed_never_exceeds_available_pending() {
        repository.save(pendingAt(NOW));
        repository.save(pendingAt(NOW.plusSeconds(1)));

        assertThat(repository.claimPending("worker-1", NOW.plusSeconds(5), 100)).hasSize(2);
    }

    @Test
    void claimPending_second_worker_cannot_claim_already_claimed_events() {
        repository.save(pendingAt(NOW));

        List<OutboxEvent> first = repository.claimPending("worker-A", NOW.plusSeconds(1), 10);
        List<OutboxEvent> second = repository.claimPending("worker-B", NOW.plusSeconds(2), 10);

        assertThat(first).hasSize(1);
        assertThat(second).isEmpty();
    }

    @Test
    void claimPending_concurrent_workers_receive_disjoint_sets() throws InterruptedException {
        for (int i = 0; i < 4; i++) {
            repository.save(pendingAt(NOW.plusSeconds(i)));
        }

        AtomicReference<List<OutboxEvent>> resultA = new AtomicReference<>(List.of());
        AtomicReference<List<OutboxEvent>> resultB = new AtomicReference<>(List.of());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch done = new CountDownLatch(2);

        new Thread(() -> {
            ready.countDown();
            try { ready.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            resultA.set(repository.claimPending("worker-A", NOW.plusSeconds(10), 3));
            done.countDown();
        }).start();

        new Thread(() -> {
            ready.countDown();
            try { ready.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            resultB.set(repository.claimPending("worker-B", NOW.plusSeconds(10), 3));
            done.countDown();
        }).start();

        done.await();

        Set<UUID> idsA = resultA.get().stream().map(OutboxEvent::eventId).collect(Collectors.toSet());
        Set<UUID> idsB = resultB.get().stream().map(OutboxEvent::eventId).collect(Collectors.toSet());

        assertThat(idsA).doesNotContainAnyElementsOf(idsB);
        assertThat(idsA.size() + idsB.size()).isEqualTo(4);
    }

    private OutboxEvent createEvent(PublishStatus status) {
        return new OutboxEvent(
                UUID.randomUUID(), "agg-" + System.identityHashCode(new Object()),
                "Order", "OrderCreatedDomainEvent", "{}", "JSON",
                NOW, status == PublishStatus.PUBLISHED ? NOW : null,
                status, 0, null, null, null,
                MessageNature.EVENT, null, null);
    }

    private OutboxEvent pendingAt(Instant occurredAt) {
        return new OutboxEvent(
                UUID.randomUUID(), "agg-" + UUID.randomUUID(),
                "Order", "OrderCreatedDomainEvent", "{}", "JSON",
                occurredAt, null, PublishStatus.PENDING, 0, null, null, null,
                MessageNature.EVENT, null, null);
    }
}
