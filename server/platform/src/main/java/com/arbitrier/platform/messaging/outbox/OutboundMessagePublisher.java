package com.arbitrier.platform.messaging.outbox;

import java.util.concurrent.CompletionStage;

/**
 * Runtime-neutral port for dispatching a persisted outbound message to its transport.
 *
 * <p>An outbox drainer reads {@link OutboxEvent} rows in {@link PublishStatus#PENDING} state
 * and calls this port to hand each message to the underlying transport (e.g. Kafka). The
 * transport adapter implements this interface; no Kafka, Avro, or broker-specific types appear
 * here.
 *
 * <p>The method is named {@code publish} rather than {@code send} to express intent without
 * implying point-to-point delivery. The port handles both domain events and commands
 * ({@link MessageNature}) through the same call, keeping routing decisions in the adapter.
 *
 * <h2>Asynchronous contract</h2>
 * <p>The returned {@link CompletionStage} completes normally when the transport confirms
 * successful publication, and completes exceptionally when the transport reports a delivery
 * failure. Callers must not block on the stage; the outbox drainer uses completion callbacks
 * to call {@link OutboxRepository#markPublished(java.util.UUID)} or
 * {@link OutboxRepository#markFailed(java.util.UUID)}.
 *
 * <h2>Why {@code CompletionStage} and not {@code CompletableFuture} or a custom type</h2>
 * <ul>
 *   <li>{@code CompletionStage} is a JDK standard — no additional dependency.</li>
 *   <li>It expresses asynchronous completion without exposing the concrete future type
 *       ({@code CompletableFuture}, {@code ListenableFuture}, etc.).</li>
 *   <li>Kafka-specific types such as {@code SendResult} do not appear in this interface.</li>
 *   <li>Implementations retain freedom over the concrete return type as long as it
 *       implements {@code CompletionStage<Void>}.</li>
 * </ul>
 *
 * <h2>Failure semantics</h2>
 * <p>Immediate failures (null argument, routing strategy error) may throw directly before
 * a stage is returned. Asynchronous transport failures complete the returned stage
 * exceptionally; they are never silently swallowed.
 *
 * <p>Layer: platform/messaging/outbox
 * <p>Module: platform
 */
public interface OutboundMessagePublisher {

    /**
     * Publish a single persisted outbound message to the runtime transport.
     *
     * @param message the outbox message to dispatch; must not be null
     * @return a {@link CompletionStage} that completes normally on broker acknowledgement
     *         or exceptionally on transport failure
     */
    CompletionStage<Void> publish(OutboxEvent message);
}
