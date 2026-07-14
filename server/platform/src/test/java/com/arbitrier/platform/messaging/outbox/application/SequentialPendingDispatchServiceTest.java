package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SequentialPendingDispatchService}.
 * OutboxRepository and DispatchOutboxMessageService are mocked — no broker, no DB.
 */
@ExtendWith(MockitoExtension.class)
class SequentialPendingDispatchServiceTest {

    @Mock
    OutboxRepository outboxRepository;

    @Mock
    DispatchOutboxMessageService dispatcher;

    private SequentialPendingDispatchService service;

    @BeforeEach
    void setUp() {
        service = new SequentialPendingDispatchService(outboxRepository, dispatcher);
        lenient().when(outboxRepository.findPending(anyInt())).thenReturn(List.of());
        lenient().when(dispatcher.dispatch(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void rejects_negative_limit() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.dispatchPending(-1))
                .withMessageContaining("negative");
    }

    @Test
    void zero_limit_returns_completed_stage_without_accessing_repository() {
        var result = service.dispatchPending(0).toCompletableFuture();

        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
        verify(outboxRepository, never()).findPending(anyInt());
        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void empty_repository_returns_completed_stage_without_dispatch() {
        when(outboxRepository.findPending(5)).thenReturn(List.of());

        var result = service.dispatchPending(5).toCompletableFuture();

        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void single_message_is_dispatched() {
        OutboxEvent event = event();
        when(outboxRepository.findPending(10)).thenReturn(List.of(event));

        service.dispatchPending(10);

        verify(dispatcher).dispatch(event);
    }

    @Test
    void multiple_messages_dispatched_in_repository_order() {
        OutboxEvent e1 = event();
        OutboxEvent e2 = event();
        OutboxEvent e3 = event();
        when(outboxRepository.findPending(10)).thenReturn(List.of(e1, e2, e3));

        service.dispatchPending(10);

        InOrder inOrder = inOrder(dispatcher);
        inOrder.verify(dispatcher).dispatch(e1);
        inOrder.verify(dispatcher).dispatch(e2);
        inOrder.verify(dispatcher).dispatch(e3);
    }

    @Test
    void passes_limit_to_repository() {
        service.dispatchPending(7);

        verify(outboxRepository).findPending(7);
    }

    @Test
    void first_dispatch_failure_stops_remaining_dispatches() {
        OutboxEvent e1 = event();
        OutboxEvent e2 = event();
        OutboxEvent e3 = event();
        when(outboxRepository.findPending(10)).thenReturn(List.of(e1, e2, e3));
        when(dispatcher.dispatch(e2)).thenReturn(
                CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));

        var result = service.dispatchPending(10).toCompletableFuture();

        assertThat(result.isCompletedExceptionally()).isTrue();
        verify(dispatcher).dispatch(e1);
        verify(dispatcher).dispatch(e2);
        verify(dispatcher, never()).dispatch(e3);
    }

    @Test
    void all_messages_dispatched_completes_normally() {
        OutboxEvent e1 = event();
        OutboxEvent e2 = event();
        when(outboxRepository.findPending(5)).thenReturn(List.of(e1, e2));

        var result = service.dispatchPending(5).toCompletableFuture();

        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private OutboxEvent event() {
        return new OutboxEvent(
                UUID.randomUUID(), "agg-" + UUID.randomUUID(), "Order",
                "OrderCreatedDomainEvent", "{}", "JSON",
                Instant.now(), null, PublishStatus.PENDING, 0, null,
                null, null, MessageNature.EVENT);
    }
}
