package com.arbitrier.platform.messaging.retry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ScheduledRetryScheduler}.
 */
@ExtendWith(MockitoExtension.class)
class ScheduledRetrySchedulerTest {

    @Mock
    ScheduledExecutorService executor;

    // ── construction ──────────────────────────────────────────────────────────

    @Test
    void rejects_null_executor() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ScheduledRetryScheduler(null))
                .withMessageContaining("executor");
    }

    // ── null guards on schedule() ─────────────────────────────────────────────

    @Test
    void rejects_null_action() {
        var scheduler = new ScheduledRetryScheduler(executor);
        assertThatNullPointerException()
                .isThrownBy(() -> scheduler.schedule(null, BackoffDelay.ZERO))
                .withMessageContaining("action");
    }

    @Test
    void rejects_null_delay() {
        var scheduler = new ScheduledRetryScheduler(executor);
        assertThatNullPointerException()
                .isThrownBy(() -> scheduler.schedule(() -> CompletableFuture.completedFuture("x"), null))
                .withMessageContaining("delay");
    }

    // ── immediate execution (BackoffDelay.ZERO) ───────────────────────────────

    @Test
    void zero_delay_executes_action_without_scheduling() {
        var scheduler = new ScheduledRetryScheduler(executor);
        var result = new CompletableFuture<String>();
        Supplier<java.util.concurrent.CompletionStage<String>> action = () -> result;

        scheduler.schedule(action, BackoffDelay.ZERO);

        verify(executor, never()).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    void zero_delay_returns_action_stage_directly() {
        var scheduler = new ScheduledRetryScheduler(executor);
        var expected = CompletableFuture.completedFuture("done");

        var returned = scheduler.schedule(() -> expected, BackoffDelay.ZERO);

        assertThat(returned).isSameAs(expected);
    }

    @Test
    void zero_delay_propagates_successful_result() {
        var scheduler = new ScheduledRetryScheduler(executor);

        var stage = scheduler.schedule(
                () -> CompletableFuture.completedFuture("value"),
                BackoffDelay.ZERO);

        assertThat(stage.toCompletableFuture().join()).isEqualTo("value");
    }

    // ── delayed retry ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    @Test
    void non_zero_delay_submits_to_executor() {
        when(executor.schedule(any(Runnable.class), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        var scheduler = new ScheduledRetryScheduler(executor);

        scheduler.schedule(() -> CompletableFuture.completedFuture("x"),
                BackoffDelay.ofMillis(500));

        verify(executor).schedule(any(Runnable.class), eq(500L), eq(TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unchecked")
    @Test
    void scheduler_executes_action_exactly_once() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        when(executor.schedule(captor.capture(), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        var scheduler = new ScheduledRetryScheduler(executor);
        var callCount = new int[]{0};

        scheduler.schedule(() -> {
            callCount[0]++;
            return CompletableFuture.completedFuture(null);
        }, BackoffDelay.ofMillis(100));

        captor.getValue().run();

        assertThat(callCount[0]).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void delayed_execution_propagates_successful_result() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        when(executor.schedule(captor.capture(), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        var scheduler = new ScheduledRetryScheduler(executor);

        var stage = scheduler.<String>schedule(
                () -> CompletableFuture.completedFuture("retried"),
                BackoffDelay.ofMillis(200));

        captor.getValue().run();

        assertThat(stage.toCompletableFuture().join()).isEqualTo("retried");
    }

    @SuppressWarnings("unchecked")
    @Test
    void delayed_execution_propagates_failure() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        when(executor.schedule(captor.capture(), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        var scheduler = new ScheduledRetryScheduler(executor);
        var cause = new RuntimeException("retry failed");

        var stage = scheduler.<String>schedule(
                () -> CompletableFuture.failedFuture(cause),
                BackoffDelay.ofMillis(200));

        captor.getValue().run();

        assertThat(stage.toCompletableFuture().isCompletedExceptionally()).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void action_throwing_synchronously_completes_bridge_exceptionally() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        when(executor.schedule(captor.capture(), anyLong(), any()))
                .thenReturn(mock(ScheduledFuture.class));
        var scheduler = new ScheduledRetryScheduler(executor);

        var stage = scheduler.<String>schedule(() -> {
            throw new RuntimeException("action blew up");
        }, BackoffDelay.ofMillis(100));

        captor.getValue().run();

        assertThat(stage.toCompletableFuture().isCompletedExceptionally()).isTrue();
    }
}
