package com.arbitrier.platform.messaging.outbox.spring;

import com.arbitrier.platform.messaging.outbox.application.OutboxPollingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OutboxPollingScheduler}.
 * OutboxPollingService is mocked — no Spring context, no broker, no DB.
 */
@ExtendWith(MockitoExtension.class)
class OutboxPollingSchedulerTest {

    @Mock
    OutboxPollingService pollingService;

    // ── delegation ────────────────────────────────────────────────────────────

    @Test
    void poll_invokes_pollOnce_exactly_once() {
        when(pollingService.pollOnce()).thenReturn(CompletableFuture.completedFuture(null));
        var scheduler = new OutboxPollingScheduler(pollingService);

        scheduler.poll();

        verify(pollingService, times(1)).pollOnce();
    }

    // ── async success ─────────────────────────────────────────────────────────

    @Test
    void async_success_does_not_throw_from_poll() {
        when(pollingService.pollOnce()).thenReturn(CompletableFuture.completedFuture(null));
        var scheduler = new OutboxPollingScheduler(pollingService);

        assertThatCode(scheduler::poll).doesNotThrowAnyException();
    }

    // ── async failure ─────────────────────────────────────────────────────────

    @Test
    void async_failure_is_observed_and_does_not_throw_from_poll() {
        when(pollingService.pollOnce())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));
        var scheduler = new OutboxPollingScheduler(pollingService);

        assertThatCode(scheduler::poll).doesNotThrowAnyException();
    }

    // ── immediate failure ─────────────────────────────────────────────────────

    @Test
    void immediate_pollOnce_failure_is_contained_at_scheduler_boundary() {
        when(pollingService.pollOnce()).thenThrow(new RuntimeException("poll failed immediately"));
        var scheduler = new OutboxPollingScheduler(pollingService);

        assertThatCode(scheduler::poll).doesNotThrowAnyException();
    }

    // ── resilience ────────────────────────────────────────────────────────────

    @Test
    void repeated_invocations_continue_after_async_failure() {
        when(pollingService.pollOnce())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("first failure")))
                .thenReturn(CompletableFuture.completedFuture(null));
        var scheduler = new OutboxPollingScheduler(pollingService);

        scheduler.poll();
        scheduler.poll();

        verify(pollingService, times(2)).pollOnce();
    }

    @Test
    void repeated_invocations_continue_after_immediate_failure() {
        when(pollingService.pollOnce())
                .thenThrow(new RuntimeException("immediate failure"))
                .thenReturn(CompletableFuture.completedFuture(null));
        var scheduler = new OutboxPollingScheduler(pollingService);

        scheduler.poll();
        scheduler.poll();

        verify(pollingService, times(2)).pollOnce();
    }
}
