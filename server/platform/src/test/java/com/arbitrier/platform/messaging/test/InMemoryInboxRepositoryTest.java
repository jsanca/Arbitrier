package com.arbitrier.platform.messaging.test;

import com.arbitrier.platform.messaging.inbox.InboxEvent;
import com.arbitrier.platform.messaging.inbox.ProcessingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class InMemoryInboxRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private InMemoryInboxRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryInboxRepository();
    }

    @Test
    void saves_and_finds_event() {
        UUID eventId = UUID.randomUUID();
        InboxEvent event = new InboxEvent(
                eventId, "order-service", NOW, null,
                ProcessingStatus.PENDING, null, null);
        repository.save(event);

        assertThat(repository.findById(eventId)).hasValue(event);
    }

    @Test
    void marks_event_as_processed() {
        UUID eventId = UUID.randomUUID();
        InboxEvent event = new InboxEvent(
                eventId, "order-service", NOW, null,
                ProcessingStatus.PENDING, null, null);
        repository.save(event);
        repository.markProcessed(eventId);

        assertThat(repository.findById(eventId)).hasValueSatisfying(e -> {
            assertThat(e.processingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
            assertThat(e.processedAt()).isNotNull();
        });
    }

    @Test
    void markProcessed_throws_for_unknown_event() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> repository.markProcessed(UUID.randomUUID()))
                .withMessageContaining("No inbox event found");
    }

    @Test
    void findById_returns_empty_for_unknown() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void replace_removes_previous_version_on_save() {
        UUID eventId = UUID.randomUUID();
        repository.save(new InboxEvent(
                eventId, "svc", NOW, null, ProcessingStatus.PENDING, null, null));
        repository.save(new InboxEvent(
                eventId, "svc", NOW, null, ProcessingStatus.PROCESSED, null, "hash"));

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().getFirst().processingStatus())
                .isEqualTo(ProcessingStatus.PROCESSED);
    }

    @Test
    void clear_removes_all_events() {
        repository.save(new InboxEvent(
                UUID.randomUUID(), "svc", NOW, null,
                ProcessingStatus.PENDING, null, null));
        repository.save(new InboxEvent(
                UUID.randomUUID(), "svc2", NOW, null,
                ProcessingStatus.PENDING, null, null));
        repository.clear();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void findAll_returns_copy() {
        repository.save(new InboxEvent(
                UUID.randomUUID(), "svc", NOW, null,
                ProcessingStatus.PENDING, null, null));
        var snapshot = repository.findAll();
        repository.clear();
        assertThat(snapshot).hasSize(1);
    }
}
