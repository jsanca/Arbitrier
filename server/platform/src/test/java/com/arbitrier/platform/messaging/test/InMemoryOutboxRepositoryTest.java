package com.arbitrier.platform.messaging.test;

import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
                MessageNature.EVENT);
        repository.save(v1);

        OutboxEvent v2 = new OutboxEvent(
                eventId, "agg-1", "Order", "Event", "v2", "JSON",
                NOW, null, PublishStatus.PUBLISHED, 1, NOW, null, null,
                MessageNature.EVENT);
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

    private OutboxEvent createEvent(PublishStatus status) {
        return new OutboxEvent(
                UUID.randomUUID(), "agg-" + System.identityHashCode(new Object()),
                "Order", "OrderCreatedDomainEvent", "{}", "JSON",
                NOW, status == PublishStatus.PUBLISHED ? NOW : null,
                status, 0, null, null, null,
                MessageNature.EVENT);
    }
}
