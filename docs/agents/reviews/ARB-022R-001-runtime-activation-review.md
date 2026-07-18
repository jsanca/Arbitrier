TASK: ARB-022R-001 — Deep Architecture Review: ARB-022.4 Runtime Activation
DATE: 2026-07-14
AGENT: clio

EXECUTIVE SUMMARY
─────────────────
ARB-022.4 is the correct and minimal runtime activation layer for the outbox
polling pipeline. The three sub-slices (overlap prevention, Spring scheduling
adapter, activation configuration) form a clean, composable whole. The
architecture respects dependency direction, isolates scheduling from business
logic, and correctly guards against misconfiguration through conditional
activation. One mild finding (unused `enabled` field at the scheduler level) and
one informational observation (no `@PreDestroy` for graceful shutdown) are
noted.

OVERALL VERDICT: PASS. Ready for ARB-022.5.

────────────────────────────────────────────────────────────────────────────────
ARCHITECTURE ASSESSMENT
────────────────────────────────────────────────────────────────────────────────

The runtime stack (top to bottom):

  OutboxPollingScheduler        [spring/]  @Scheduled, failure containment
      │ calls pollOnce()
      ▼
  OutboxPollingService          [application/]  AtomicBoolean overlap guard,
      │                                         batch-size, stateful cycle
      │ calls dispatchPending(batchSize)
      ▼
  SequentialPendingDispatchService  [application/]  findPending + thenCompose fold
      │ calls dispatch(event) for each pending
      ▼
  DispatchOutboxMessageService  [application/]  publish → markPublished/markFailed
      │ calls publish(event)
      ▼
  OutboundMessagePublisher      [outbox/ port]  transport-neutral contract
      │ implemented by
      ▼
  KafkaOutboundMessagePublisher [kafka/ adapter]  KafkaTemplate<String,String>

Dependency direction: spring → application → ports → adapter. All inward.
No upward dependencies. No cycles.

Each layer owns exactly one lifecycle concern:
  - Scheduler: timing, fixed-delay, async failure observation
  - Polling service: overlap prevention (AtomicBoolean), batch-size, cycle state
  - Sequential dispatch: batch iteration, ordering, first-failure stop
  - Single-message dispatch: publish → markPublished/markFailed composition
  - Publisher: Kafka transport, serialization, routing, headers

No class owns more than one responsibility. The boundary between timing
(scheduler) and behavior (polling service) is clean — the scheduler knows
WHEN to call pollOnce(); the polling service knows WHETHER to start a cycle.

────────────────────────────────────────────────────────────────────────────────
FINDINGS
────────────────────────────────────────────────────────────────────────────────

─── 1. OutboxPollingService — Overlap Prevention ──────────────────────────────

FINDING F1 — [POSITIVE] AtomicBoolean overlap guard is correct and complete
Category: Architecture
Severity: INFO (positive)

Verified the overlap prevention mechanism (lines 77-89):

  if (!running.compareAndSet(false, true)) {
      return CompletableFuture.completedFuture(null);  // skip — cycle in flight
  }
  try {
      cycle = sequentialDispatch.dispatchPending(batchSize);
  } catch (RuntimeException immediate) {
      running.set(false);                              // CLEAR on synchronous throw
      throw immediate;
  }
  return cycle.whenComplete((result, ex) -> running.set(false));  // CLEAR on async completion

All three paths clear the flag:
  1. Normal async completion → whenComplete fires → flag cleared
  2. Exceptional async completion → whenComplete fires (failure param non-null) → flag cleared
  3. Synchronous throw from delegate → catch block → flag cleared before rethrow

No path permanently locks the flag. Subsequent pollOnce() calls always recover.

Tests verified (13 tests):
  - concurrent_poll_returns_immediately_without_dispatch: second call during active
    cycle returns already-completed stage, no duplicate dispatch
  - dispatcher_invoked_only_once_when_overlap_occurs: three concurrent calls → one dispatch
  - running_flag_cleared_after_success/failure/immediate_exception: all three
    recovery paths verified

This correctly addresses the ARB-022.3 review Finding W1 (HIGH: overlapping cycles
cause duplicate dispatch).

─── 2. OutboxPollingScheduler — Spring Integration ────────────────────────────

FINDING F2 — [POSITIVE] Fixed-delay with failure containment is correct
Category: Architecture
Severity: INFO (positive)

Verified the scheduler:

  @Scheduled(fixedDelayString = "${...fixed-delay-ms:10000}",
             initialDelayString = "${...initial-delay-ms:5000}")
  public void poll() {
      try {
          pollingService.pollOnce().whenComplete((ignored, failure) -> {
              if (failure != null) log.error(...);
          });
      } catch (RuntimeException ex) {
          log.error(...);
      }
  }

Design choices verified:
  - Fixed-delay (not fixed-rate): interval measured from completion of previous
    cycle. Correct — avoids queued ticks during slow cycles. With the overlap
    guard already in place, fixed-rate ticks would all be skipped anyway,
    generating noise.
  - Non-blocking: pollOnce() returns CompletionStage; poll() returns immediately.
    No get()/join() calls. Correct — doesn't hold the scheduler thread.
  - Async failure containment: whenComplete logs at ERROR, does not rethrow.
    Future ticks continue. Correct — one failed cycle doesn't kill the scheduler.
  - Sync failure containment: try-catch logs at ERROR, does not rethrow.
    Future ticks continue. Correct.

Tests verified (6 tests):
  - delegation count, async success, async failure containment, sync failure
    containment, continued operation after both failure types

FINDING F3 — [LOW] @Scheduled inline defaults duplicate OutboxPollingProperties
  Java defaults
Category: Architecture
Severity: LOW

Evidence: @Scheduled annotation has inline defaults (10000, 5000) that match
OutboxPollingProperties.java field defaults. @Scheduled reads from the Spring
Environment, not from @ConfigurationProperties. If someone changes
OutboxPollingProperties.defaultFixedDelayMs but doesn't update the @Scheduled
default, they diverge silently.

Impact: Low. The @Scheduled annotation read the same property key
(arbitrier.messaging.outbox.polling.fixed-delay-ms) that would populate
OutboxPollingProperties. The inline defaults are only fallbacks. If someone
removes or renames the property, @Scheduled falls back to the hardcoded
default, not the Java default. Non-obvious to a developer reading only the
@ConfigurationProperties class.

Recommendation: Accept as-is. This is a known Spring pattern.
@ConfigurationProperties provides centralized documentation and defaults for
enabled/batchSize; @Scheduled has its own property resolution with inline
fallback. Document this in the @Scheduled Javadoc if confusion is anticipated.
Fix now or future slice: Accept (informational).
Blocks roadmap: No.

─── 3. OutboxPollingProperties — Configuration Model ──────────────────────────

FINDING F4 — [POSITIVE] Clean namespace, correct defaults, proper ownership
Category: Architecture
Severity: INFO (positive)

Verified:
  - Namespace: arbitrier.messaging.outbox.polling — consistent with the
    arbitrier.kafka.topics.* convention
  - 4 properties: enabled (boolean), initial-delay-ms (long), fixed-delay-ms
    (long), batch-size (int)
  - Defaults: enabled=true (activate on startup), initial-delay=5s (grace
    period for app startup), fixed-delay=10s (don't hammer the broker),
    batch-size=100 (reasonable chunk size)
  - @ConfigurationProperties — single source of truth for all polling tuning.
    No scattered @Value annotations.
  - Property ownership: belongs to platform.messaging.outbox.spring — the
    Spring integration layer. Correct.

Test coverage: 5 unit tests verify defaults and setter mutation.

─── 4. OutboxSchedulingAutoConfiguration — Bean Graph ─────────────────────────

FINDING F5 — [POSITIVE] Conditional chain defends against misconfiguration
Category: Architecture
Severity: INFO (positive)

Verified the activation chain:

  @AutoConfiguration
  @EnableScheduling
  @EnableConfigurationProperties(OutboxPollingProperties.class)
  @ConditionalOnProperty(prefix="arbitrier.messaging.outbox.polling",
                          name="enabled", havingValue="true", matchIfMissing=true)
  @ConditionalOnBean(SequentialPendingDispatchService.class)
  public class OutboxSchedulingAutoConfiguration { ... }

Activation requires BOTH conditions:
  1. Property enabled=true (or absent → matchIfMissing=true)
  2. SequentialPendingDispatchService bean present (implies dispatch chain exists)

Bean creation (inside the class, only active when both conditions met):
  - outboxPollingService: binds batchSize from properties to OutboxPollingService
  - outboxPollingScheduler: binds polling service to OutboxPollingScheduler

Both beans are @ConditionalOnMissingBean — services can override.

@EnableScheduling: scoped to this configuration only. No scheduling is
enabled in contexts that don't start the outbox pipeline. Correct isolation.

AutoConfiguration.imports: OutboxSchedulingAutoConfiguration registered at
line 3 (after PlatformAutoConfiguration, KafkaPublisherAutoConfiguration).
Order doesn't matter — all auto-configurations are independent.

FINDING F6 — [INFO] No graceful shutdown (@PreDestroy) handling
Category: Architecture
Severity: INFO

Evidence: OutboxPollingScheduler has no @PreDestroy method. If the application
is shutting down while a poll cycle is in progress, the in-flight Kafka sends
and DB status updates may be interrupted. The AtomicBoolean guard clears on
cycle completion, but a mid-cycle shutdown leaves it set (JVM dies before
whenComplete fires).

Impact: Low for development. A mid-shutdown cycle may leave events in
PENDING status — they will be redispatched on the next startup (duplicate
publication → inbox dedup catches it). No data loss, just wasted resources.

Recommendation: Add @PreDestroy with a brief grace period (e.g., 30s wait on
active cycle, then log and give up):
  if (pollingService.hasActiveCycle()) {
      log.info("Waiting for active polling cycle to complete...");
      // await with timeout
  }
This is a production concern. Acceptable to defer.
Fix now or future slice: Future slice (ARB-022.5 or dedicated operational slice).
Blocks roadmap: No.

─── 5. Overall Runtime — Cohesion & Understandability ─────────────────────────

FINDING F7 — [POSITIVE] A new engineer can trace the full flow without hidden
  coupling

Verified: The full dispatch path is:

  OutboxPollingScheduler.poll()                       [@Scheduled tick]
    → OutboxPollingService.pollOnce()                 [AtomicBoolean guard → skip if running]
        → SequentialPendingDispatchService.dispatchPending(batchSize)
            → OutboxRepository.findPending(limit)      [SELECT ... WHERE status=PENDING LIMIT N]
            → for each event: thenCompose → DispatchOutboxMessageService.dispatch(event)
                → OutboundMessagePublisher.publish(event)  [transport port]
                    → KafkaOutboundMessagePublisher    [Kafka adapter]
                → markPublished(eventId) or markFailed(eventId)

Each step is a single class with one method to understand. No hidden state
machines, no reflection, no framework magic beyond @Scheduled. The AtomicBoolean
is the only mutable state, and its lifecycle is documented in 13 tests.

No coupling exists between:
  - Scheduler timing and batch size
  - Overlap prevention and Kafka transport
  - Batch iteration and message serialization
  - Status updates and routing strategy

All coupling is through constructor-injected ports.

─── 6. Readiness for ARB-022.5 ────────────────────────────────────────────────

The polling runtime foundation is complete. ARB-022.5 can build on:

| Capability | Status | What ARB-022.5 needs to add |
|---|---|---|
| Single-worker polling | DONE | No changes needed |
| Batch dispatch with overlap guard | DONE | No changes needed |
| Configurable batch size | DONE | No changes needed |
| Scheduler with fixed-delay | DONE | No changes needed |
| Failure containment (async + sync) | DONE | ARB-022.5 may add retry with backoff |
| Property-based activation | DONE | No changes needed |
| Graceful shutdown | NOT DONE | @PreDestroy with cycle-completion wait (see F6) |
| Metrics/tracing per cycle | NOT DONE | Cycle duration, success/failure count, skip count |

────────────────────────────────────────────────────────────────────────────────
VERDICT
────────────────────────────────────────────────────────────────────────────────

OVERALL: PASS

  [INFO]  F1 — AtomicBoolean overlap guard correct and complete (positive)
  [INFO]  F2 — Fixed-delay with failure containment correct (positive)
  [LOW]   F3 — @Scheduled inline defaults duplicate OutboxPollingProperties
  [INFO]  F4 — Clean namespace, correct defaults, proper ownership (positive)
  [INFO]  F5 — Conditional chain defends against misconfiguration (positive)
  [INFO]  F6 — No @PreDestroy graceful shutdown handling
  [INFO]  F7 — Full dispatch path is traceable without hidden coupling (positive)

7 findings: 6 INFO (5 positive, 1 informational), 1 LOW. No FAIL. No WARN.

ARB-022.4 IS READY to be considered the canonical polling runtime foundation
for the remaining messaging roadmap.
