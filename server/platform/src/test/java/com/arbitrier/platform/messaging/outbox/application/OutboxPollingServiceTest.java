package com.arbitrier.platform.messaging.outbox.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OutboxPollingService}.
 * SequentialPendingDispatchService is mocked — no broker, no DB, no scheduler.
 */
@ExtendWith(MockitoExtension.class)
class OutboxPollingServiceTest {

    @Mock
    SequentialPendingDispatchService sequentialDispatch;

    // ── construction ─────────────────────────────────────────────────────────

    @Test
    void valid_batch_size_accepted() {
        var service = new OutboxPollingService(sequentialDispatch, 10);
        assertThat(service).isNotNull();
    }

    @Test
    void zero_batch_size_rejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxPollingService(sequentialDispatch, 0))
                .withMessageContaining("batchSize");
    }

    @Test
    void negative_batch_size_rejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OutboxPollingService(sequentialDispatch, -1))
                .withMessageContaining("batchSize");
    }

    // ── basic delegation ──────────────────────────────────────────────────────

    @Test
    void first_poll_executes_and_delegates() {
        when(sequentialDispatch.dispatchPending(5))
                .thenReturn(CompletableFuture.completedFuture(null));
        var service = new OutboxPollingService(sequentialDispatch, 5);

        service.pollOnce();

        verify(sequentialDispatch, times(1)).dispatchPending(5);
    }

    @Test
    void configured_batch_size_is_passed_to_dispatchPending() {
        when(sequentialDispatch.dispatchPending(42))
                .thenReturn(CompletableFuture.completedFuture(null));
        var service = new OutboxPollingService(sequentialDispatch, 42);

        service.pollOnce();

        verify(sequentialDispatch).dispatchPending(42);
    }

    // ── overlap prevention ────────────────────────────────────────────────────

    @Test
    void concurrent_poll_returns_immediately_without_dispatch() {
        // Hold the first cycle open with an incomplete future
        CompletableFuture<Void> firstCycle = new CompletableFuture<>();
        when(sequentialDispatch.dispatchPending(10)).thenReturn(firstCycle);
        var service = new OutboxPollingService(sequentialDispatch, 10);

        service.pollOnce(); // starts first cycle (not yet complete)

        // Second call while first is still running
        var secondResult = service.pollOnce().toCompletableFuture();

        assertThat(secondResult.isDone()).isTrue();
        assertThat(secondResult.isCompletedExceptionally()).isFalse();
        verify(sequentialDispatch, times(1)).dispatchPending(10); // only once
    }

    @Test
    void dispatcher_invoked_only_once_when_overlap_occurs() {
        CompletableFuture<Void> firstCycle = new CompletableFuture<>();
        when(sequentialDispatch.dispatchPending(10)).thenReturn(firstCycle);
        var service = new OutboxPollingService(sequentialDispatch, 10);

        service.pollOnce();
        service.pollOnce();
        service.pollOnce();

        verify(sequentialDispatch, times(1)).dispatchPending(10);
    }

    // ── flag cleared after completion ─────────────────────────────────────────

    @Test
    void running_flag_cleared_after_success_so_next_poll_executes() {
        when(sequentialDispatch.dispatchPending(10))
                .thenReturn(CompletableFuture.completedFuture(null));
        var service = new OutboxPollingService(sequentialDispatch, 10);

        service.pollOnce(); // completes immediately
        service.pollOnce(); // should also execute

        verify(sequentialDispatch, times(2)).dispatchPending(10);
    }

    @Test
    void running_flag_cleared_after_failure_so_next_poll_executes() {
        when(sequentialDispatch.dispatchPending(10))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("dispatch failed")));
        var service = new OutboxPollingService(sequentialDispatch, 10);

        service.pollOnce(); // completes exceptionally
        service.pollOnce(); // should also execute

        verify(sequentialDispatch, times(2)).dispatchPending(10);
    }

    @Test
    void running_flag_cleared_after_immediate_exception() {
        when(sequentialDispatch.dispatchPending(10))
                .thenThrow(new RuntimeException("synchronous failure"))
                .thenReturn(CompletableFuture.completedFuture(null));
        var service = new OutboxPollingService(sequentialDispatch, 10);

        try { service.pollOnce(); } catch (RuntimeException ignored) { }
        service.pollOnce(); // should execute after flag is cleared

        verify(sequentialDispatch, times(2)).dispatchPending(10);
    }

    // ── failure propagation ───────────────────────────────────────────────────

    @Test
    void successful_stage_is_propagated() {
        when(sequentialDispatch.dispatchPending(10))
                .thenReturn(CompletableFuture.completedFuture(null));
        var service = new OutboxPollingService(sequentialDispatch, 10);

        var result = service.pollOnce().toCompletableFuture();

        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
    }

    @Test
    void exceptional_stage_is_propagated() {
        when(sequentialDispatch.dispatchPending(10))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("dispatch failed")));
        var service = new OutboxPollingService(sequentialDispatch, 10);

        var result = service.pollOnce().toCompletableFuture();

        assertThat(result.isCompletedExceptionally()).isTrue();
    }

    @Test
    void immediate_delegate_failure_is_propagated() {
        when(sequentialDispatch.dispatchPending(10))
                .thenThrow(new RuntimeException("synchronous failure"));
        var service = new OutboxPollingService(sequentialDispatch, 10);

        assertThatThrownBy(service::pollOnce)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("synchronous failure");
    }
}
