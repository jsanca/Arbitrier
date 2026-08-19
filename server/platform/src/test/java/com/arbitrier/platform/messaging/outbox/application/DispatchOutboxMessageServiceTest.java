package com.arbitrier.platform.messaging.outbox.application;

import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.OutboundMessagePublisher;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import com.arbitrier.platform.messaging.retry.BackoffDelay;
import com.arbitrier.platform.messaging.retry.BackoffStrategy;
import com.arbitrier.platform.messaging.retry.DeadMessageContext;
import com.arbitrier.platform.messaging.retry.DeadMessageHandler;
import com.arbitrier.platform.messaging.retry.RetryPolicy;
import com.arbitrier.platform.messaging.retry.RetryScheduler;
import com.arbitrier.platform.messaging.retry.SimpleRetryPolicy;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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

        var result = service.dispatch(eventMessage()).toCompletableFuture();

        assertThat(result.isCompletedExceptionally()).isTrue();
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

    // ── scheduler integration ─────────────────────────────────────────────────

    @Mock
    RetryScheduler retryScheduler;

    @Mock
    BackoffStrategy backoffStrategy;

    @Mock
    DeadMessageHandler deadMessageHandler;

    @Test
    void dispatcher_calls_scheduler_on_retry() {
        // policy: retry once; scheduler: execute action immediately
        RetryPolicy retryOnce = new SimpleRetryPolicy(2);
        lenient().when(backoffStrategy.nextDelay(any(int.class))).thenReturn(BackoffDelay.ZERO);
        doAnswer(inv -> {
            Supplier<?> action = inv.getArgument(0);
            return action.get();
        }).when(retryScheduler).schedule(any(), any());

        // first call fails, second succeeds
        when(publisher.publish(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("transient")))
                .thenReturn(CompletableFuture.completedFuture(null));

        var service = new DispatchOutboxMessageService(
                publisher, outboxRepository, retryOnce, backoffStrategy, retryScheduler);

        service.dispatch(eventMessage()).toCompletableFuture().join();

        verify(retryScheduler, times(1)).schedule(any(), any());
        verify(outboxRepository).markPublished(any(UUID.class));
        verify(outboxRepository, never()).markFailed(any());
    }

    @Test
    void dispatcher_honors_backoff_delay_from_strategy() {
        RetryPolicy retryOnce = new SimpleRetryPolicy(2);
        BackoffDelay expectedDelay = BackoffDelay.ofMillis(750);
        when(backoffStrategy.nextDelay(2)).thenReturn(expectedDelay);
        doAnswer(inv -> {
            Supplier<?> action = inv.getArgument(0);
            return action.get();
        }).when(retryScheduler).schedule(any(), any());
        when(publisher.publish(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("transient")))
                .thenReturn(CompletableFuture.completedFuture(null));

        var service = new DispatchOutboxMessageService(
                publisher, outboxRepository, retryOnce, backoffStrategy, retryScheduler);

        service.dispatch(eventMessage()).toCompletableFuture().join();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<BackoffDelay> delayCaptor = ArgumentCaptor.forClass(BackoffDelay.class);
        verify(retryScheduler).schedule(any(), delayCaptor.capture());
        assertThat(delayCaptor.getValue()).isEqualTo(expectedDelay);
    }

    // ── dead message handler integration ─────────────────────────────────────

    @Test
    void handler_invoked_after_final_async_failure() {
        when(deadMessageHandler.handle(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(publisher.publish(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("permanent")));

        var service = new DispatchOutboxMessageService(
                publisher, outboxRepository,
                new SimpleRetryPolicy(1),
                attempt -> BackoffDelay.ZERO,
                immediateScheduler(),
                deadMessageHandler);

        service.dispatch(eventMessage()).toCompletableFuture();

        verify(deadMessageHandler, times(1)).handle(any(DeadMessageContext.class));
    }

    @Test
    void handler_not_invoked_during_retries() {
        doAnswer(inv -> {
            Supplier<?> action = inv.getArgument(0);
            return action.get();
        }).when(retryScheduler).schedule(any(), any());
        // First two attempts fail, third succeeds → handler never called
        when(publisher.publish(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("transient")))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("transient")))
                .thenReturn(CompletableFuture.completedFuture(null));

        var service = new DispatchOutboxMessageService(
                publisher, outboxRepository,
                new SimpleRetryPolicy(3),
                attempt -> BackoffDelay.ZERO,
                retryScheduler,
                deadMessageHandler);

        service.dispatch(eventMessage()).toCompletableFuture().join();

        verify(deadMessageHandler, never()).handle(any());
    }

    @Test
    void handler_receives_correct_context() {
        RuntimeException cause = new RuntimeException("permanent broker error");
        when(publisher.publish(any()))
                .thenReturn(CompletableFuture.failedFuture(cause));
        ArgumentCaptor<DeadMessageContext> ctxCaptor =
                ArgumentCaptor.forClass(DeadMessageContext.class);
        when(deadMessageHandler.handle(ctxCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(null));

        var msg = eventMessage();
        var service = new DispatchOutboxMessageService(
                publisher, outboxRepository,
                new SimpleRetryPolicy(1),
                attempt -> BackoffDelay.ZERO,
                immediateScheduler(),
                deadMessageHandler);

        service.dispatch(msg).toCompletableFuture();

        DeadMessageContext ctx = ctxCaptor.getValue();
        assertThat(ctx.totalAttempts()).isEqualTo(1);
        assertThat(ctx.cause()).isSameAs(cause);
        assertThat(ctx.message()).isSameAs(msg);
        assertThat(ctx.failedAt()).isNotNull();
    }

    @Test
    void handler_not_invoked_on_successful_dispatch() {
        when(publisher.publish(any())).thenReturn(CompletableFuture.completedFuture(null));

        var service = new DispatchOutboxMessageService(
                publisher, outboxRepository,
                new SimpleRetryPolicy(1),
                attempt -> BackoffDelay.ZERO,
                immediateScheduler(),
                deadMessageHandler);

        service.dispatch(eventMessage()).toCompletableFuture().join();

        verify(deadMessageHandler, never()).handle(any());
    }

    @Test
    void dispatch_stage_completes_exceptionally_after_handler() {
        RuntimeException cause = new RuntimeException("fatal");
        when(publisher.publish(any())).thenReturn(CompletableFuture.failedFuture(cause));
        when(deadMessageHandler.handle(any())).thenReturn(CompletableFuture.completedFuture(null));

        var service = new DispatchOutboxMessageService(
                publisher, outboxRepository,
                new SimpleRetryPolicy(1),
                attempt -> BackoffDelay.ZERO,
                immediateScheduler(),
                deadMessageHandler);

        var result = service.dispatch(eventMessage()).toCompletableFuture();

        assertThat(result.isCompletedExceptionally()).isTrue();
    }

    @Test
    void dispatcher_does_not_call_scheduler_when_policy_says_stop() {
        RetryPolicy noRetry = new SimpleRetryPolicy(1);
        when(publisher.publish(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("fail")));

        var service = new DispatchOutboxMessageService(
                publisher, outboxRepository, noRetry, backoffStrategy, retryScheduler);

        service.dispatch(eventMessage()).toCompletableFuture();

        verify(retryScheduler, never()).schedule(any(), any());
        verify(outboxRepository).markFailed(any(UUID.class));
    }

    // ── metrics recorder integration ─────────────────────────────────────────

    @Mock
    DispatchMetricsRecorder metricsRecorder;

    @Test
    void recorder_started_and_succeeded_called_on_success() {
        when(publisher.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
        var svc = new DispatchOutboxMessageService(
                publisher, outboxRepository,
                new SimpleRetryPolicy(1),
                attempt -> BackoffDelay.ZERO,
                immediateScheduler(),
                deadMessageHandler,
                metricsRecorder);
        lenient().when(deadMessageHandler.handle(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        svc.dispatch(eventMessage()).toCompletableFuture().join();

        verify(metricsRecorder).recordStarted(any());
        verify(metricsRecorder).recordSucceeded(any(), any(Duration.class));
        verify(metricsRecorder, never()).recordRetry(any(), anyInt());
        verify(metricsRecorder, never()).recordDead(any(), anyInt(), any());
    }

    @Test
    void recorder_retry_called_on_each_retry_attempt() {
        doAnswer(inv -> {
            Supplier<?> action = inv.getArgument(0);
            return action.get();
        }).when(retryScheduler).schedule(any(), any());
        when(publisher.publish(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("t")))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("t")))
                .thenReturn(CompletableFuture.completedFuture(null));

        var svc = new DispatchOutboxMessageService(
                publisher, outboxRepository,
                new SimpleRetryPolicy(3),
                attempt -> BackoffDelay.ZERO,
                retryScheduler,
                deadMessageHandler,
                metricsRecorder);
        lenient().when(deadMessageHandler.handle(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        svc.dispatch(eventMessage()).toCompletableFuture().join();

        verify(metricsRecorder, times(2)).recordRetry(any(), anyInt());
        verify(metricsRecorder).recordSucceeded(any(), any(Duration.class));
        verify(metricsRecorder, never()).recordDead(any(), anyInt(), any());
    }

    @Test
    void recorder_dead_called_on_terminal_failure() {
        when(publisher.publish(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("permanent")));
        when(deadMessageHandler.handle(any())).thenReturn(CompletableFuture.completedFuture(null));

        var svc = new DispatchOutboxMessageService(
                publisher, outboxRepository,
                new SimpleRetryPolicy(1),
                attempt -> BackoffDelay.ZERO,
                immediateScheduler(),
                deadMessageHandler,
                metricsRecorder);

        svc.dispatch(eventMessage()).toCompletableFuture();

        verify(metricsRecorder).recordDead(any(), eq(1), any(Duration.class));
        verify(metricsRecorder, never()).recordSucceeded(any(), any());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private RetryScheduler immediateScheduler() {
        return new RetryScheduler() {
            @Override
            public <T> java.util.concurrent.CompletionStage<T> schedule(
                    Supplier<java.util.concurrent.CompletionStage<T>> action,
                    BackoffDelay delay) {
                return action.get();
            }
        };
    }

    private OutboxEvent eventMessage() {
        return new OutboxEvent(
                UUID.randomUUID(), "order-001", "Order", "OrderCreatedDomainEvent",
                "{}", "JSON", Instant.now(), null, PublishStatus.PENDING, 0, null,
                null, null, MessageNature.EVENT, null, null);
    }
}
