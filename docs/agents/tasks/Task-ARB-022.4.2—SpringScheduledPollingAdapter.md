Task: ARB-022.4.2 — Spring Scheduled Polling Adapter

Status:
[PLANNED]

Owner:
Clio

Role:
Implementation

Timebox:
Target: 15–25 minutes
Hard stop: 45 minutes

Apply:

.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

Do not load unrelated UI or Motion skills.

If incomplete at the hard stop, create:

docs/agents/checkpoints/CHECKPOINT-ARB-022.4.2.md

and stop.

--------------------------------------------------

Context

Completed:

ARB-022.3.3
OutboxPollingService

ARB-022.4.1
Polling Cycle Overlap Prevention

OutboxPollingService now:

- represents one bounded polling cycle;
- owns the configured batch size;
- prevents overlapping cycles within one JVM;
- returns CompletionStage<Void>;
- preserves success and failure outcomes.

The next step is to invoke this behavior through Spring scheduling.

This slice introduces the scheduling adapter only.

Runtime property binding and complete activation policy remain separate unless
the minimum configuration is required to make the scheduler usable.

--------------------------------------------------

Goal

Create a Spring infrastructure adapter that periodically invokes:

OutboxPollingService.pollOnce()

The scheduler must contain no polling, dispatch, repository, Kafka, overlap,
retry, or business logic.

Expected responsibility:

Spring clock
↓
scheduled method
↓
OutboxPollingService.pollOnce()

--------------------------------------------------

1. Scheduled Adapter

Create a focused Spring component.

Suggested name:

OutboxPollingScheduler

Suggested package direction:

com.arbitrier.platform.messaging.outbox.spring

or:

com.arbitrier.platform.messaging.outbox.scheduling

Prefer the existing infrastructure-package convention.

The scheduler depends only on:

OutboxPollingService

No direct dependencies on:

- OutboxRepository
- SequentialPendingDispatchService
- DispatchOutboxMessageService
- OutboundMessagePublisher
- KafkaTemplate

--------------------------------------------------

2. Scheduled Method

Create one scheduled method.

Suggested name:

poll()

or:

triggerPollingCycle()

Use Spring scheduling infrastructure.

Expected shape:

@Scheduled(
fixedDelayString = "${arbitrier.messaging.outbox.polling.fixed-delay-ms:10000}",
initialDelayString = "${arbitrier.messaging.outbox.polling.initial-delay-ms:5000}"
)
public void poll() {
pollingService.pollOnce();
}

The exact syntax may follow current Spring Boot 4.1 conventions.

Do not block on the returned CompletionStage.

Do not call:

- get()
- join()
- sleep()

Overlap protection already belongs to OutboxPollingService.

--------------------------------------------------

3. Completion and Failure Observation

The scheduled method must observe asynchronous failures so they are not silently
discarded.

Preferred minimal direction:

pollingService.pollOnce()
.whenComplete((ignored, failure) -> {
if (failure != null) {
log the cycle failure safely;
}
});

Requirements:

- do not rethrow asynchronous failures from the callback;
- do not terminate future scheduled execution;
- do not retry inside the scheduler;
- preserve safe logging rules;
- do not log message payloads or sensitive metadata.

Use the existing platform logging conventions.

If an immediate exception is thrown by pollOnce():

- catch it at the scheduler boundary;
- log it safely;
- allow future scheduled invocations to continue.

The scheduler is an operational boundary and should not die because one cycle
fails.

--------------------------------------------------

4. Fixed Delay Semantics

Use fixed delay, not fixed rate.

Rationale:

- fixed delay better represents recurring polling;
- cycle overlap is already prevented by OutboxPollingService;
- the scheduler does not need to compensate for missed ticks;
- fixed-rate semantics could generate repeated skipped invocations during a
  long-running cycle.

Document the choice.

Do not implement custom rescheduling with TaskScheduler yet.

--------------------------------------------------

5. Scheduling Activation

Register scheduling support using the smallest existing Spring mechanism.

Before adding a new global:

@EnableScheduling

inspect whether scheduling is already enabled elsewhere.

Requirements:

- avoid duplicate or scattered @EnableScheduling declarations;
- prefer platform auto-configuration if that matches the repository convention;
- do not enable unrelated schedulers accidentally without documenting the
  consequence.

If scheduling requires dedicated auto-configuration, keep it narrowly scoped.

--------------------------------------------------

6. Conditional Bean Creation

The scheduler should only exist when the polling runtime is available.

At minimum, condition it on:

OutboxPollingService

Prefer:

@ConditionalOnBean(OutboxPollingService.class)

Do not depend directly on Kafka availability; that dependency is already part
of the lower-level runtime wiring.

Do not create a fake or no-op polling service.

Full enabled/disabled property support belongs to ARB-022.4.3 unless a minimal
condition is necessary now.

--------------------------------------------------

7. Logging

Use an existing logger convention.

Recommended levels:

- DEBUG or TRACE for successful or skipped routine cycles, if logged at all;
- WARN or ERROR for failed cycles according to existing platform policy.

Avoid noisy success logs on every polling tick.

Never log:

- payloads
- credentials
- tokens
- full serialized events
- sensitive customer metadata

The scheduler may log a concise failure type and safe message.

--------------------------------------------------

8. Tests

Add focused tests for the adapter.

Cover:

- scheduled method invokes pollOnce exactly once;
- asynchronous success does not produce an exception;
- asynchronous failure is observed and does not throw from the scheduled method;
- immediate pollOnce failure is contained at the scheduler boundary;
- scheduler has no dependency beyond OutboxPollingService;
- repeated manual invocations continue after a failed cycle.

Do not test Spring's clock itself.

A plain unit test is preferred.

Add a minimal Spring context test only if required to validate conditional bean
registration or scheduling metadata.

No Kafka.
No database.
No Docker.
No sleeping or timing-based tests.

--------------------------------------------------

9. Architecture Constraints

Verify:

- scheduler is an infrastructure adapter;
- polling behavior remains in OutboxPollingService;
- overlap prevention is not duplicated;
- scheduler does not inspect active/running state;
- scheduler does not know batch size;
- scheduler does not own retries;
- no transport or persistence details leak into it.

Add no new abstraction unless an actual need appears.

--------------------------------------------------

10. Documentation

Create:

docs/agents/reports/ARB-022.4.2-spring-scheduled-polling-adapter.md

Document:

- class and package;
- scheduled method;
- fixed-delay decision;
- initial and fixed-delay defaults;
- failure-observation behavior;
- activation conditions;
- scheduling-enablement decision;
- tests and validation performed;
- concerns intentionally deferred to ARB-022.4.3.

Update canonical implementation documentation only if required to keep current
behavior accurate.

--------------------------------------------------

Out of Scope

Do NOT implement:

- polling configuration-properties class
- broad runtime property model
- enabled/disabled property unless strictly required
- graceful shutdown waiting
- TaskScheduler customization
- dynamic schedule changes
- overlap state
- distributed coordination
- CLAIMED / IN_PROGRESS
- retries
- backoff
- DLQ
- metrics
- tracing
- queue monitoring
- Kafka changes
- Avro
- UI or Motion integration

--------------------------------------------------

Acceptance Criteria

✓ Spring scheduler adapter exists.

✓ Scheduled method invokes OutboxPollingService only.

✓ Fixed-delay scheduling is used.

✓ Initial delay and fixed delay are externally configurable through minimal
property placeholders.

✓ Scheduler does not block on CompletionStage.

✓ Immediate and asynchronous cycle failures are safely contained and observed.

✓ Overlap logic is not duplicated.

✓ Scheduler activation is conditional on the polling runtime.

✓ No repository, Kafka, or dispatch internals leak into the scheduler.

✓ Focused tests pass.

✓ Completion report exists.

Do not begin ARB-022.4.3.