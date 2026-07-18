package com.arbitrier.platform.messaging.outbox.adapter;

import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import com.arbitrier.platform.time.FixedTimeProvider;
import com.arbitrier.platform.time.TimeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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

@SpringBootTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.flyway.enabled=false"
        }
)
@Testcontainers
class JpaOutboxRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withInitScript("test-db/create-platform-schema.sql");

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class MinimalConfig {
    }

    @Configuration
    static class TestConfig {
        @Bean
        TimeProvider timeProvider() {
            return FixedTimeProvider.of(NOW);
        }

        /**
         * Registers the adapter as a Spring-managed bean so that {@code @Transactional}
         * on {@code claimPending} is honoured by the AOP proxy.
         */
        @Bean
        OutboxRepository outboxRepository(SpringDataOutboxRepository repo, TimeProvider timeProvider) {
            return new JpaOutboxRepositoryAdapter(repo, timeProvider);
        }
    }

    @Autowired
    private SpringDataOutboxRepository springDataRepo;

    @Autowired
    private OutboxRepository adapter;

    @AfterEach
    void tearDown() {
        springDataRepo.deleteAll();
    }

    @Test
    @Transactional
    void saves_event_and_finds_it_as_pending() {
        OutboxEvent event = createPendingEvent();
        adapter.save(event);

        List<OutboxEvent> pending = adapter.findPending();
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().eventId()).isEqualTo(event.eventId());
        assertThat(pending.getFirst().publishStatus()).isEqualTo(PublishStatus.PENDING);
    }

    @Test
    @Transactional
    void marks_event_as_published() {
        OutboxEvent event = createPendingEvent();
        adapter.save(event);

        adapter.markPublished(event.eventId());

        assertThat(adapter.findPending()).isEmpty();
    }

    @Test
    @Transactional
    void marks_event_as_failed_and_increments_attempt_count() {
        OutboxEvent event = createPendingEvent();
        adapter.save(event);

        adapter.markFailed(event.eventId());

        assertThat(adapter.findPending()).isEmpty();
    }

    @Test
    @Transactional
    void markPublished_throws_for_unknown_event() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> adapter.markPublished(UUID.randomUUID()))
                .withMessageContaining("No outbox event found");
    }

    @Test
    @Transactional
    void markFailed_throws_for_unknown_event() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> adapter.markFailed(UUID.randomUUID()))
                .withMessageContaining("No outbox event found");
    }

    @Test
    @Transactional
    void findPending_excludes_published_and_failed() {
        OutboxEvent pending = createPendingEvent();
        adapter.save(pending);

        OutboxEvent published = createPendingEvent();
        adapter.save(published);
        adapter.markPublished(published.eventId());

        OutboxEvent failed = createPendingEvent();
        adapter.save(failed);
        adapter.markFailed(failed.eventId());

        assertThat(adapter.findPending()).hasSize(1);
        assertThat(adapter.findPending().getFirst().eventId()).isEqualTo(pending.eventId());
    }

    @Test
    @Transactional
    void command_nature_survives_round_trip() {
        OutboxEvent command = createPendingCommand();
        adapter.save(command);

        List<OutboxEvent> pending = adapter.findPending();
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().eventId()).isEqualTo(command.eventId());
        assertThat(pending.getFirst().messageNature()).isEqualTo(MessageNature.COMMAND);
    }

    @Test
    @Transactional
    void findPending_with_limit_returns_empty_when_no_pending_events() {
        assertThat(adapter.findPending(10)).isEmpty();
    }

    @Test
    @Transactional
    void findPending_with_limit_returns_single_pending_event() {
        adapter.save(createPendingEvent(NOW));

        List<OutboxEvent> result = adapter.findPending(10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().publishStatus()).isEqualTo(PublishStatus.PENDING);
    }

    @Test
    @Transactional
    void findPending_with_limit_1_returns_oldest_first() {
        OutboxEvent oldest = createPendingEvent(NOW);
        OutboxEvent newest = createPendingEvent(NOW.plusSeconds(2));
        OutboxEvent middle = createPendingEvent(NOW.plusSeconds(1));
        adapter.save(newest);
        adapter.save(oldest);
        adapter.save(middle);

        List<OutboxEvent> result = adapter.findPending(2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).eventId()).isEqualTo(oldest.eventId());
        assertThat(result.get(1).eventId()).isEqualTo(middle.eventId());
    }

    @Test
    @Transactional
    void findPending_with_limit_larger_than_available_returns_all() {
        adapter.save(createPendingEvent(NOW));
        adapter.save(createPendingEvent(NOW.plusSeconds(1)));

        assertThat(adapter.findPending(100)).hasSize(2);
    }

    @Test
    @Transactional
    void findPending_with_limit_0_returns_empty() {
        adapter.save(createPendingEvent(NOW));

        assertThat(adapter.findPending(0)).isEmpty();
    }

    @Test
    void findPending_with_negative_limit_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> adapter.findPending(-1))
                .withMessageContaining("negative");
    }

    @Test
    @Transactional
    void findPending_with_limit_excludes_published_and_failed() {
        OutboxEvent pending = createPendingEvent(NOW);
        adapter.save(pending);

        OutboxEvent published = createPendingEvent(NOW.plusSeconds(1));
        adapter.save(published);
        adapter.markPublished(published.eventId());

        OutboxEvent failed = createPendingEvent(NOW.plusSeconds(2));
        adapter.save(failed);
        adapter.markFailed(failed.eventId());

        List<OutboxEvent> result = adapter.findPending(10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().eventId()).isEqualTo(pending.eventId());
    }

    @Test
    @Transactional
    void claimed_state_survives_round_trip() {
        OutboxEvent claimed = createClaimedEvent("worker-42", NOW.plusSeconds(1));
        adapter.save(claimed);

        OutboxEventEntity entity = springDataRepo.findById(claimed.eventId()).orElseThrow();
        assertThat(entity.getPublishStatus()).isEqualTo("CLAIMED");
        assertThat(entity.getClaimedBy()).isEqualTo("worker-42");
        assertThat(entity.getClaimedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    @Transactional
    void findPending_excludes_claimed() {
        OutboxEvent pending = createPendingEvent(NOW);
        adapter.save(pending);

        OutboxEvent claimed = createClaimedEvent("worker-1", NOW.plusSeconds(1));
        adapter.save(claimed);

        List<OutboxEvent> result = adapter.findPending();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().eventId()).isEqualTo(pending.eventId());
    }

    @Test
    @Transactional
    void findPending_with_limit_excludes_claimed() {
        OutboxEvent pending = createPendingEvent(NOW);
        adapter.save(pending);

        OutboxEvent claimed = createClaimedEvent("worker-1", NOW.plusSeconds(1));
        adapter.save(claimed);

        List<OutboxEvent> result = adapter.findPending(10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().eventId()).isEqualTo(pending.eventId());
    }

    private OutboxEvent createPendingEvent() {
        return createPendingEvent(NOW);
    }

    private OutboxEvent createPendingEvent(Instant occurredAt) {
        return new OutboxEvent(
                UUID.randomUUID(), "agg-" + UUID.randomUUID(),
                "Order", "OrderCreatedDomainEvent",
                "{\"test\":true}", "JSON",
                occurredAt, null, PublishStatus.PENDING, 0, null, null, null,
                MessageNature.EVENT, null, null);
    }

    // ── claimEvent ────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void claimEvent_succeeds_on_pending_event() {
        OutboxEvent pending = createPendingEvent();
        adapter.save(pending);

        Optional<OutboxEvent> result = adapter.claimEvent(pending.eventId(), "worker-1", NOW.plusSeconds(1));

        assertThat(result).isPresent();
        assertThat(result.get().publishStatus()).isEqualTo(PublishStatus.CLAIMED);
        assertThat(result.get().claimedBy()).isEqualTo("worker-1");
        assertThat(result.get().claimedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    @Transactional
    void claimEvent_persists_claim_metadata() {
        OutboxEvent pending = createPendingEvent();
        adapter.save(pending);
        adapter.claimEvent(pending.eventId(), "worker-42", NOW.plusSeconds(2));

        OutboxEventEntity entity = springDataRepo.findById(pending.eventId()).orElseThrow();
        assertThat(entity.getPublishStatus()).isEqualTo("CLAIMED");
        assertThat(entity.getClaimedBy()).isEqualTo("worker-42");
        assertThat(entity.getClaimedAt()).isEqualTo(NOW.plusSeconds(2));
    }

    @Test
    @Transactional
    void claimEvent_returns_empty_if_already_claimed() {
        OutboxEvent claimed = createClaimedEvent("worker-1", NOW);
        adapter.save(claimed);

        Optional<OutboxEvent> result = adapter.claimEvent(claimed.eventId(), "worker-2", NOW.plusSeconds(1));

        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void claimEvent_returns_empty_if_published() {
        adapter.save(createPendingEvent());
        OutboxEvent pending = adapter.findPending().getFirst();
        adapter.markPublished(pending.eventId());

        Optional<OutboxEvent> result = adapter.claimEvent(pending.eventId(), "worker-1", NOW);

        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void claimEvent_returns_empty_if_failed() {
        adapter.save(createPendingEvent());
        OutboxEvent pending = adapter.findPending().getFirst();
        adapter.markFailed(pending.eventId());

        Optional<OutboxEvent> result = adapter.claimEvent(pending.eventId(), "worker-1", NOW);

        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void claimEvent_returns_empty_if_not_found() {
        Optional<OutboxEvent> result = adapter.claimEvent(UUID.randomUUID(), "worker-1", NOW);

        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void claimEvent_round_trip_preserves_all_event_fields() {
        OutboxEvent pending = createPendingEvent();
        adapter.save(pending);

        OutboxEvent claimed = adapter.claimEvent(pending.eventId(), "worker-1", NOW.plusSeconds(1)).orElseThrow();

        assertThat(claimed.eventId()).isEqualTo(pending.eventId());
        assertThat(claimed.aggregateId()).isEqualTo(pending.aggregateId());
        assertThat(claimed.aggregateType()).isEqualTo(pending.aggregateType());
        assertThat(claimed.eventType()).isEqualTo(pending.eventType());
        assertThat(claimed.payload()).isEqualTo(pending.payload());
        assertThat(claimed.payloadFormat()).isEqualTo(pending.payloadFormat());
        assertThat(claimed.occurredAt()).isEqualTo(pending.occurredAt());
        assertThat(claimed.messageNature()).isEqualTo(pending.messageNature());
        assertThat(claimed.publishStatus()).isEqualTo(PublishStatus.CLAIMED);
        assertThat(claimed.claimedBy()).isEqualTo("worker-1");
        assertThat(claimed.claimedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void claimEvent_concurrent_exactly_one_winner() throws InterruptedException {
        // Save and commit outside any transaction so both threads can see the row.
        OutboxEvent pending = createPendingEvent(NOW);
        springDataRepo.save(toEntity(pending));

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch done = new CountDownLatch(2);

        for (int i = 0; i < 2; i++) {
            String workerId = "worker-" + i;
            new Thread(() -> {
                ready.countDown();
                try { ready.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                Optional<OutboxEvent> result = adapter.claimEvent(pending.eventId(), workerId, NOW.plusSeconds(1));
                if (result.isPresent()) successCount.incrementAndGet();
                done.countDown();
            }).start();
        }

        done.await();
        assertThat(successCount.get()).isEqualTo(1);
    }

    // ── claimPending (batch) ──────────────────────────────────────────────────

    @Test
    @Transactional
    void claimPending_returns_empty_when_no_pending_events() {
        assertThat(adapter.claimPending("worker-1", NOW, 10)).isEmpty();
    }

    @Test
    @Transactional
    void claimPending_claims_single_pending_event() {
        adapter.save(createPendingEvent(NOW));

        List<OutboxEvent> result = adapter.claimPending("worker-1", NOW.plusSeconds(1), 10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().publishStatus()).isEqualTo(PublishStatus.CLAIMED);
        assertThat(result.getFirst().claimedBy()).isEqualTo("worker-1");
        assertThat(result.getFirst().claimedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    @Transactional
    void claimPending_preserves_fifo_order() {
        OutboxEvent oldest = createPendingEvent(NOW);
        OutboxEvent middle = createPendingEvent(NOW.plusSeconds(1));
        OutboxEvent newest = createPendingEvent(NOW.plusSeconds(2));
        adapter.save(newest);
        adapter.save(oldest);
        adapter.save(middle);

        List<OutboxEvent> result = adapter.claimPending("worker-1", NOW.plusSeconds(5), 10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).eventId()).isEqualTo(oldest.eventId());
        assertThat(result.get(1).eventId()).isEqualTo(middle.eventId());
        assertThat(result.get(2).eventId()).isEqualTo(newest.eventId());
    }

    @Test
    @Transactional
    void claimPending_respects_limit() {
        adapter.save(createPendingEvent(NOW));
        adapter.save(createPendingEvent(NOW.plusSeconds(1)));
        adapter.save(createPendingEvent(NOW.plusSeconds(2)));

        List<OutboxEvent> result = adapter.claimPending("worker-1", NOW.plusSeconds(5), 2);

        assertThat(result).hasSize(2);
    }

    @Test
    @Transactional
    void claimPending_limit_zero_returns_empty() {
        adapter.save(createPendingEvent(NOW));

        assertThat(adapter.claimPending("worker-1", NOW, 0)).isEmpty();
    }

    @Test
    void claimPending_negative_limit_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> adapter.claimPending("worker-1", NOW, -1))
                .withMessageContaining("negative");
    }

    @Test
    void claimPending_blank_workerId_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> adapter.claimPending("  ", NOW, 10))
                .withMessageContaining("workerId");
    }

    @Test
    void claimPending_null_claimedAt_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> adapter.claimPending("worker-1", null, 10))
                .withMessageContaining("claimedAt");
    }

    @Test
    @Transactional
    void claimPending_excludes_published_events() {
        OutboxEvent pending = createPendingEvent(NOW);
        adapter.save(pending);
        OutboxEvent published = createPendingEvent(NOW.plusSeconds(1));
        adapter.save(published);
        adapter.markPublished(published.eventId());

        List<OutboxEvent> result = adapter.claimPending("worker-1", NOW.plusSeconds(5), 10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().eventId()).isEqualTo(pending.eventId());
    }

    @Test
    @Transactional
    void claimPending_excludes_failed_events() {
        OutboxEvent pending = createPendingEvent(NOW);
        adapter.save(pending);
        OutboxEvent failed = createPendingEvent(NOW.plusSeconds(1));
        adapter.save(failed);
        adapter.markFailed(failed.eventId());

        List<OutboxEvent> result = adapter.claimPending("worker-1", NOW.plusSeconds(5), 10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().eventId()).isEqualTo(pending.eventId());
    }

    @Test
    @Transactional
    void claimPending_excludes_already_claimed_events() {
        OutboxEvent pending = createPendingEvent(NOW);
        adapter.save(pending);
        OutboxEvent alreadyClaimed = createClaimedEvent("other-worker", NOW.plusSeconds(1));
        adapter.save(alreadyClaimed);

        List<OutboxEvent> result = adapter.claimPending("worker-1", NOW.plusSeconds(5), 10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().eventId()).isEqualTo(pending.eventId());
    }

    @Test
    @Transactional
    void claimPending_persists_claim_metadata_on_all_claimed_events() {
        adapter.save(createPendingEvent(NOW));
        adapter.save(createPendingEvent(NOW.plusSeconds(1)));

        List<OutboxEvent> result = adapter.claimPending("worker-batch", NOW.plusSeconds(5), 10);

        assertThat(result).hasSize(2);
        result.forEach(e -> {
            assertThat(e.claimedBy()).isEqualTo("worker-batch");
            assertThat(e.claimedAt()).isEqualTo(NOW.plusSeconds(5));
            assertThat(e.publishStatus()).isEqualTo(PublishStatus.CLAIMED);
        });
    }

    @Test
    @Transactional
    void claimPending_total_claimed_never_exceeds_available_pending() {
        adapter.save(createPendingEvent(NOW));
        adapter.save(createPendingEvent(NOW.plusSeconds(1)));

        List<OutboxEvent> result = adapter.claimPending("worker-1", NOW.plusSeconds(5), 100);

        assertThat(result).hasSize(2);
    }

    @Test
    void claimPending_second_worker_cannot_claim_already_claimed_events() {
        // Committed outside a transaction so both calls see the row
        springDataRepo.save(toEntity(createPendingEvent(NOW)));

        List<OutboxEvent> first = adapter.claimPending("worker-A", NOW.plusSeconds(1), 10);
        List<OutboxEvent> second = adapter.claimPending("worker-B", NOW.plusSeconds(2), 10);

        assertThat(first).hasSize(1);
        assertThat(second).isEmpty();
    }

    @Test
    void claimPending_concurrent_workers_receive_disjoint_sets() throws InterruptedException {
        for (int i = 0; i < 4; i++) {
            springDataRepo.save(toEntity(createPendingEvent(NOW.plusSeconds(i))));
        }

        AtomicReference<List<OutboxEvent>> resultA = new AtomicReference<>(List.of());
        AtomicReference<List<OutboxEvent>> resultB = new AtomicReference<>(List.of());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch done = new CountDownLatch(2);

        new Thread(() -> {
            ready.countDown();
            try { ready.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            resultA.set(adapter.claimPending("worker-A", NOW.plusSeconds(10), 3));
            done.countDown();
        }).start();

        new Thread(() -> {
            ready.countDown();
            try { ready.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            resultB.set(adapter.claimPending("worker-B", NOW.plusSeconds(10), 3));
            done.countDown();
        }).start();

        done.await();

        Set<UUID> idsA = resultA.get().stream().map(OutboxEvent::eventId).collect(Collectors.toSet());
        Set<UUID> idsB = resultB.get().stream().map(OutboxEvent::eventId).collect(Collectors.toSet());

        assertThat(idsA).doesNotContainAnyElementsOf(idsB);
        assertThat(idsA.size() + idsB.size()).isEqualTo(4);
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
        entity.setClaimedBy(event.claimedBy());
        entity.setClaimedAt(event.claimedAt());
        return entity;
    }

    private OutboxEvent createClaimedEvent(String workerId, Instant claimedAt) {
        return new OutboxEvent(
                UUID.randomUUID(), "agg-" + UUID.randomUUID(),
                "Order", "OrderCreatedDomainEvent",
                "{\"test\":true}", "JSON",
                NOW, null, PublishStatus.CLAIMED, 0, null, null, null,
                MessageNature.EVENT, workerId, claimedAt);
    }

    private OutboxEvent createPendingCommand() {
        return new OutboxEvent(
                UUID.randomUUID(), "saga-" + UUID.randomUUID(),
                "Saga", "ReserveStockCommand",
                "{\"test\":true}", "JSON",
                NOW, null, PublishStatus.PENDING, 0, null, null, null,
                MessageNature.COMMAND, null, null);
    }
}
