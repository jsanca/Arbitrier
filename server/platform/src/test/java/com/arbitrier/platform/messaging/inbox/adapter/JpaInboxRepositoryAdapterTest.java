package com.arbitrier.platform.messaging.inbox.adapter;

import com.arbitrier.platform.messaging.inbox.InboxEvent;
import com.arbitrier.platform.messaging.inbox.InboxRepository;
import com.arbitrier.platform.messaging.inbox.ProcessingStatus;
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
import java.util.Optional;
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
class JpaInboxRepositoryAdapterTest {

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
    private SpringDataInboxRepository springDataRepo;

    @Autowired
    private TimeProvider timeProvider;

    private InboxRepository adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaInboxRepositoryAdapter(springDataRepo, timeProvider);
    }

    @Test
    @Transactional
    void saves_event_and_finds_by_id() {
        UUID eventId = UUID.randomUUID();
        InboxEvent event = createPendingEvent(eventId);
        adapter.save(event);

        Optional<InboxEvent> found = adapter.findById(eventId);
        assertThat(found).isPresent();
        assertThat(found.get().processingStatus()).isEqualTo(ProcessingStatus.PENDING);
    }

    @Test
    @Transactional
    void marks_event_as_processed() {
        UUID eventId = UUID.randomUUID();
        adapter.save(createPendingEvent(eventId));

        adapter.markProcessed(eventId);

        Optional<InboxEvent> found = adapter.findById(eventId);
        assertThat(found).hasValueSatisfying(e -> {
            assertThat(e.processingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
            assertThat(e.processedAt()).isNotNull();
        });
    }

    @Test
    @Transactional
    void findById_returns_empty_for_unknown() {
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    @Transactional
    void markProcessed_throws_for_unknown_event() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> adapter.markProcessed(UUID.randomUUID()))
                .withMessageContaining("No inbox event found");
    }

    @Test
    @Transactional
    void saves_multiple_events_and_finds_each() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        adapter.save(createPendingEvent(id1));
        adapter.save(createPendingEvent(id2));

        assertThat(adapter.findById(id1)).isPresent();
        assertThat(adapter.findById(id2)).isPresent();
    }

    private InboxEvent createPendingEvent(UUID eventId) {
        return new InboxEvent(
                eventId, "test-consumer", NOW, null,
                ProcessingStatus.PENDING, null, null);
    }
}
