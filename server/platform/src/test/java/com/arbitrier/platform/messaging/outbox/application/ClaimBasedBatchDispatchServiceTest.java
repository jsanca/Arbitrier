package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import com.arbitrier.platform.time.FixedTimeProvider;
import com.arbitrier.platform.time.TimeProvider;
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
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClaimBasedBatchDispatchService}.
 * OutboxRepository and DispatchOutboxMessageService are mocked — no broker, no DB.
 */
@ExtendWith(MockitoExtension.class)
class ClaimBasedBatchDispatchServiceTest {

    static final String WORKER_ID = "worker-test-01";
    static final Instant NOW = Instant.parse("2026-07-17T10:00:00Z");

    @Mock
    OutboxRepository outboxRepository;

    @Mock
    DispatchOutboxMessageService dispatcher;

    TimeProvider timeProvider = FixedTimeProvider.of(NOW);

    private ClaimBasedBatchDispatchService service;

    @BeforeEach
    void setUp() {
        service = new ClaimBasedBatchDispatchService(outboxRepository, dispatcher, timeProvider, WORKER_ID);
        lenient().when(outboxRepository.claimPending(anyString(), any(), anyInt())).thenReturn(List.of());
        lenient().when(dispatcher.dispatch(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    // ── construction ─────────────────────────────────────────────────────────

    @Test
    void null_repository_rejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ClaimBasedBatchDispatchService(null, dispatcher, timeProvider, WORKER_ID));
    }

    @Test
    void null_dispatcher_rejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ClaimBasedBatchDispatchService(outboxRepository, null, timeProvider, WORKER_ID));
    }

    @Test
    void null_time_provider_rejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ClaimBasedBatchDispatchService(outboxRepository, dispatcher, null, WORKER_ID));
    }

    @Test
    void blank_worker_id_rejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ClaimBasedBatchDispatchService(outboxRepository, dispatcher, timeProvider, "  "));
    }

    @Test
    void worker_id_is_accessible() {
        assertThat(service.workerId()).isEqualTo(WORKER_ID);
    }

    // ── limit guards ──────────────────────────────────────────────────────────

    @Test
    void negative_limit_rejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.dispatchClaimed(-1))
                .withMessageContaining("negative");
    }

    @Test
    void zero_limit_returns_completed_stage_without_claiming() {
        var result = service.dispatchClaimed(0).toCompletableFuture();

        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
        verify(outboxRepository, never()).claimPending(anyString(), any(), anyInt());
        verify(dispatcher, never()).dispatch(any());
    }

    // ── claiming ──────────────────────────────────────────────────────────────

    @Test
    void passes_worker_id_and_timestamp_to_claimPending() {
        service.dispatchClaimed(5);

        verify(outboxRepository).claimPending(WORKER_ID, NOW, 5);
    }

    @Test
    void passes_limit_to_claimPending() {
        service.dispatchClaimed(7);

        verify(outboxRepository).claimPending(anyString(), any(), eq(7));
    }

    @Test
    void empty_claim_result_returns_completed_stage_without_dispatch() {
        when(outboxRepository.claimPending(anyString(), any(), anyInt())).thenReturn(List.of());

        var result = service.dispatchClaimed(5).toCompletableFuture();

        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
        verify(dispatcher, never()).dispatch(any());
    }

    // ── dispatch ──────────────────────────────────────────────────────────────

    @Test
    void single_claimed_event_is_dispatched() {
        OutboxEvent event = claimedEvent();
        when(outboxRepository.claimPending(anyString(), any(), anyInt())).thenReturn(List.of(event));

        service.dispatchClaimed(10);

        verify(dispatcher).dispatch(event);
    }

    @Test
    void multiple_claimed_events_dispatched_in_order() {
        OutboxEvent e1 = claimedEvent();
        OutboxEvent e2 = claimedEvent();
        OutboxEvent e3 = claimedEvent();
        when(outboxRepository.claimPending(anyString(), any(), anyInt())).thenReturn(List.of(e1, e2, e3));

        service.dispatchClaimed(10);

        InOrder inOrder = inOrder(dispatcher);
        inOrder.verify(dispatcher).dispatch(e1);
        inOrder.verify(dispatcher).dispatch(e2);
        inOrder.verify(dispatcher).dispatch(e3);
    }

    @Test
    void first_dispatch_failure_stops_remaining_dispatches() {
        OutboxEvent e1 = claimedEvent();
        OutboxEvent e2 = claimedEvent();
        OutboxEvent e3 = claimedEvent();
        when(outboxRepository.claimPending(anyString(), any(), anyInt())).thenReturn(List.of(e1, e2, e3));
        when(dispatcher.dispatch(e2)).thenReturn(
                CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));

        var result = service.dispatchClaimed(10).toCompletableFuture();

        assertThat(result.isCompletedExceptionally()).isTrue();
        verify(dispatcher).dispatch(e1);
        verify(dispatcher).dispatch(e2);
        verify(dispatcher, never()).dispatch(e3);
    }

    @Test
    void all_claimed_events_dispatched_completes_normally() {
        OutboxEvent e1 = claimedEvent();
        OutboxEvent e2 = claimedEvent();
        when(outboxRepository.claimPending(anyString(), any(), anyInt())).thenReturn(List.of(e1, e2));

        var result = service.dispatchClaimed(5).toCompletableFuture();

        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private OutboxEvent claimedEvent() {
        return new OutboxEvent(
                UUID.randomUUID(), "agg-" + UUID.randomUUID(), "Order",
                "OrderCreatedDomainEvent", "{}", "JSON",
                NOW, null, PublishStatus.CLAIMED, 0, null,
                null, null, MessageNature.EVENT, WORKER_ID, NOW);
    }
}
