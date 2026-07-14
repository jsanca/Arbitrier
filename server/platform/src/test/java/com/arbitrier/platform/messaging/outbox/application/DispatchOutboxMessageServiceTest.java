package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.OutboundMessagePublisher;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DispatchOutboxMessageService}.
 * No Spring, no broker, no Testcontainers — all ports are mocked.
 */
@ExtendWith(MockitoExtension.class)
class DispatchOutboxMessageServiceTest {

    @Mock
    OutboundMessagePublisher publisher;

    @Mock
    OutboxRepository outboxRepository;

    private DispatchOutboxMessageService service;

    @BeforeEach
    void setUp() {
        service = new DispatchOutboxMessageService(publisher, outboxRepository);
        lenient().when(publisher.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void rejects_null_message() {
        assertThatNullPointerException()
                .isThrownBy(() -> service.dispatch(null))
                .withMessageContaining("message");
    }

    @Test
    void calls_publisher_exactly_once() {
        service.dispatch(eventMessage());

        verify(publisher, times(1)).publish(any());
    }

    @Test
    void successful_publication_calls_markPublished() {
        service.dispatch(eventMessage());

        verify(outboxRepository).markPublished(any(UUID.class));
    }

    @Test
    void successful_publication_does_not_call_markFailed() {
        service.dispatch(eventMessage());

        verify(outboxRepository, never()).markFailed(any());
    }

    @Test
    void asynchronous_publication_failure_calls_markFailed() {
        when(publisher.publish(any())).thenReturn(
                CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));

        service.dispatch(eventMessage());

        verify(outboxRepository).markFailed(any(UUID.class));
        verify(outboxRepository, never()).markPublished(any());
    }

    @Test
    void asynchronous_publication_failure_remains_exceptional() {
        when(publisher.publish(any())).thenReturn(
                CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));

        var result = service.dispatch(eventMessage()).toCompletableFuture();

        assertThat(result.isCompletedExceptionally()).isTrue();
    }

    @Test
    void immediate_publication_failure_calls_markFailed() {
        when(publisher.publish(any())).thenThrow(new RuntimeException("routing error"));

        assertThatThrownBy(() -> service.dispatch(eventMessage()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("routing error");

        verify(outboxRepository).markFailed(any(UUID.class));
        verify(outboxRepository, never()).markPublished(any());
    }

    @Test
    void markFailed_failure_is_attached_as_suppressed_to_publication_failure() {
        RuntimeException pubEx = new RuntimeException("publish failed");
        RuntimeException markFailedEx = new RuntimeException("markFailed failed");
        when(publisher.publish(any())).thenReturn(CompletableFuture.failedFuture(pubEx));
        doThrow(markFailedEx).when(outboxRepository).markFailed(any());

        var result = service.dispatch(eventMessage()).toCompletableFuture();

        assertThat(result.isCompletedExceptionally()).isTrue();
        assertThat(pubEx.getSuppressed()).containsExactly(markFailedEx);
    }

    @Test
    void markPublished_failure_completes_exceptionally() {
        doThrow(new RuntimeException("DB failure")).when(outboxRepository).markPublished(any());

        var result = service.dispatch(eventMessage()).toCompletableFuture();

        assertThat(result.isCompletedExceptionally()).isTrue();
        verify(outboxRepository, never()).markFailed(any());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private OutboxEvent eventMessage() {
        return new OutboxEvent(
                UUID.randomUUID(), "order-001", "Order", "OrderCreatedDomainEvent",
                "{}", "JSON", Instant.now(), null, PublishStatus.PENDING, 0, null,
                null, null, MessageNature.EVENT);
    }
}
