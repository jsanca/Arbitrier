package com.arbitrier.platform.messaging.retry;

import java.util.concurrent.CompletionStage;

/**
 * Transport-agnostic contract for processing outbox messages that have permanently failed
 * after exhausting all configured retry attempts.
 *
 * <p>Implementations must complete the returned stage normally — they must not re-throw or
 * complete the stage exceptionally, because a handler failure would mask the original
 * publication cause. If a handler encounters an error of its own, it should log it and
 * complete normally.
 *
 * <p>Implementations must not block the calling thread or call {@link Thread#sleep}.
 *
 * <p>The dispatcher uses the completion of this stage to sequence the overall failure
 * acknowledgement, so returning immediately with a completed stage is acceptable.
 *
 * <p>Layer: platform/messaging/retry
 * <p>Module: platform
 */
public interface DeadMessageHandler {

    /**
     * Process a permanently failed outbox message.
     *
     * @param context diagnostic context for the dead message; never null
     * @return a {@link CompletionStage} that completes normally when the handler finishes;
     *         must never complete exceptionally
     */
    CompletionStage<Void> handle(DeadMessageContext context);
}
