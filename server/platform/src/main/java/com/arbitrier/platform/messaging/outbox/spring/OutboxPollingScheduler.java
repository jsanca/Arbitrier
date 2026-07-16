package com.arbitrier.platform.messaging.outbox.spring;

import com.arbitrier.platform.messaging.outbox.application.OutboxPollingService;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Spring scheduling adapter that periodically triggers one outbox polling cycle.
 *
 * <p>Invokes {@link OutboxPollingService#pollOnce()} on each tick. All polling logic,
 * batch-size knowledge, and overlap prevention reside in {@code OutboxPollingService};
 * this class owns only the scheduling boundary.
 *
 * <h2>Fixed-delay semantics</h2>
 * <p>The interval between ticks is measured from the completion of the previous cycle
 * (fixed delay), not from its start (fixed rate). This avoids queuing up back-to-back
 * invocations during a slow cycle, since {@code OutboxPollingService} already prevents
 * overlaps — repeated skipped ticks would just generate noise.
 *
 * <h2>Failure observation</h2>
 * <p>Asynchronous cycle failures are logged at ERROR level but not rethrown — future
 * scheduled ticks continue. Immediate synchronous throws from {@code pollOnce()} are
 * caught at this boundary and also logged without rethrowing.
 *
 * <p>Layer: platform/messaging/outbox/spring
 * <p>Module: platform
 */
public class OutboxPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollingScheduler.class);

    private final OutboxPollingService pollingService;

    /** Creates the scheduler bound to the given polling service. */
    public OutboxPollingScheduler(final OutboxPollingService pollingService) {
        this.pollingService = Require.notNull(pollingService, "pollingService");
    }

    /**
     * Triggers one outbox polling cycle.
     *
     * <p>Fixed delay: {@code arbitrier.messaging.outbox.polling.fixed-delay-ms} (default 10 000 ms).
     * Initial delay: {@code arbitrier.messaging.outbox.polling.initial-delay-ms} (default 5 000 ms).
     *
     * <p>The returned {@link java.util.concurrent.CompletionStage} is not joined; this method
     * returns immediately. Failures are observed via {@code whenComplete} and logged.
     */
    @Scheduled(
            fixedDelayString  = "${arbitrier.messaging.outbox.polling.fixed-delay-ms:10000}",
            initialDelayString = "${arbitrier.messaging.outbox.polling.initial-delay-ms:5000}"
    )
    public void poll() {
        try {
            pollingService.pollOnce()
                    .whenComplete((ignored, failure) -> {
                        if (failure != null) {
                            log.error("Outbox polling cycle failed asynchronously: {}",
                                    failure.getMessage(), failure);
                        }
                    });
        } catch (RuntimeException ex) {
            log.error("Outbox polling cycle failed immediately: {}", ex.getMessage(), ex);
        }
    }
}
