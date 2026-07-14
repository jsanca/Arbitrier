package com.arbitrier.platform.messaging.outbox.adapter;

import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import com.arbitrier.platform.time.FixedTimeProvider;
import com.arbitrier.platform.time.TimeProvider;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
    }

    @Autowired
    private SpringDataOutboxRepository springDataRepo;

    @Autowired
    private TimeProvider timeProvider;

    private OutboxRepository adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaOutboxRepositoryAdapter(springDataRepo, timeProvider);
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

    private OutboxEvent createPendingEvent() {
        return createPendingEvent(NOW);
    }

    private OutboxEvent createPendingEvent(Instant occurredAt) {
        return new OutboxEvent(
                UUID.randomUUID(), "agg-" + UUID.randomUUID(),
                "Order", "OrderCreatedDomainEvent",
                "{\"test\":true}", "JSON",
                occurredAt, null, PublishStatus.PENDING, 0, null, null, null,
                MessageNature.EVENT);
    }

    private OutboxEvent createPendingCommand() {
        return new OutboxEvent(
                UUID.randomUUID(), "saga-" + UUID.randomUUID(),
                "Saga", "ReserveStockCommand",
                "{\"test\":true}", "JSON",
                NOW, null, PublishStatus.PENDING, 0, null, null, null,
                MessageNature.COMMAND);
    }
}
