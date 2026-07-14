Task: ARB-022.3.3 — Pending Message Polling Facade

Status:
[PLANNED]

Owner:
Clio

Role:
Implementation

Timebox:
Target: 15–20 minutes
Hard stop: 45 minutes

Apply:

.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

If incomplete at the hard stop, create:

docs/agents/checkpoints/CHECKPOINT-ARB-022.3.3.md

and stop.

--------------------------------------------------

Context

Completed:

ARB-022.3.1
Pending Message Retrieval

ARB-022.3.2
Sequential Pending Message Dispatch

The runtime can now:

- retrieve a bounded set of pending messages;
- dispatch those messages sequentially;
- preserve repository order;
- stop on the first failure.

The next step is to expose one explicit polling-cycle operation.

This slice does not schedule that operation.

No @Scheduled.
No timer.
No infinite loop.

--------------------------------------------------

Goal

Create a small polling facade representing one bounded Outbox polling cycle.

The facade delegates to SequentialPendingDispatchService using a configured
batch limit.

Expected flow:

pollOnce()
↓
SequentialPendingDispatchService.dispatchPending(batchSize)
↓
CompletionStage<Void>

--------------------------------------------------

1. Polling Facade

Create a focused class.

Suggested name:

PendingOutboxPollingService

or:

OutboxPollingService

Prefer the name that clearly communicates:

- Outbox ownership;
- one polling responsibility;
- no scheduler ownership.

Suggested package:

com.arbitrier.platform.messaging.outbox.application

The service should depend only on:

SequentialPendingDispatchService

and a validated polling configuration/value.

No Kafka imports.
No repository dependency is required directly.

--------------------------------------------------

2. Public API

Suggested API:

CompletionStage<Void> pollOnce()

The method must:

- invoke SequentialPendingDispatchService.dispatchPending(batchSize);
- invoke it exactly once;
- return the same asynchronous lifecycle;
- not block;
- not swallow failures.

The method represents one polling cycle only.

Do not name it:

runForever()
start()
schedule()
pollContinuously()

--------------------------------------------------

3. Batch Size Configuration

Introduce an explicit batch-size input.

Preferred minimal direction:

constructor parameter:

int batchSize

Validate:

batchSize > 0

Reject zero or negative values during construction.

Do not read Spring properties directly inside the service.

Keep the service framework-neutral.

A later runtime configuration slice may bind:

arbitrier.messaging.outbox.polling.batch-size

to this constructor.

Do not add that property binding unless it is already trivial and required for
compilation.

--------------------------------------------------

4. Failure Semantics

If sequential dispatch completes normally:

pollOnce() completes normally.

If sequential dispatch:

- throws immediately; or
- returns a stage that completes exceptionally;

pollOnce() must preserve that behavior.

Do not catch and convert failures.

The scheduler/runtime layer will later decide how to log and isolate polling
cycle failures.

--------------------------------------------------

5. No Overlapping Cycle Logic

Do not implement:

- running flags;
- locks;
- semaphores;
- overlap prevention;
- distributed leases.

Those concerns belong to the scheduled runtime and concurrent-worker slices.

This service represents behavior only.

--------------------------------------------------

6. Tests

Add focused tests covering:

- valid batch size accepted;
- zero batch size rejected;
- negative batch size rejected;
- pollOnce delegates exactly once;
- configured batch size is passed to dispatchPending;
- successful stage is propagated;
- exceptional stage is propagated;
- immediate delegate failure is propagated.

No repository.
No Kafka.
No scheduler.
No Spring context test.

--------------------------------------------------

7. Architecture

Verify:

- polling facade remains framework-neutral;
- no @Scheduled annotation;
- no transport dependency;
- no persistence dependency;
- scheduling and polling behavior remain separate responsibilities.

Do not add an interface for the facade unless an actual second implementation
exists or a current test seam requires it.

--------------------------------------------------

8. Documentation

Create:

docs/agents/reports/ARB-022.3.3-pending-message-polling-facade.md

Document:

- service name and package;
- meaning of one polling cycle;
- batch-size validation;
- delegation flow;
- failure semantics;
- why scheduling is deferred;
- tests executed.

--------------------------------------------------

Out of Scope

Do NOT implement:

- @Scheduled
- Spring task scheduling
- polling intervals
- fixed delay / fixed rate
- infinite loops
- overlapping-run protection
- distributed locks
- multiple workers
- claim semantics
- retries
- backoff
- metrics
- tracing
- Kafka changes
- Avro
- configuration-property classes unless strictly necessary

--------------------------------------------------

Acceptance Criteria

✓ Polling facade exists.

✓ pollOnce() represents exactly one bounded cycle.

✓ Batch size is explicit and validated.

✓ Sequential dispatch is invoked exactly once per cycle.

✓ Completion and failure semantics are preserved.

✓ No scheduling or timing concern is introduced.

✓ Focused tests pass.

✓ Completion report exists.

Do not begin ARB-022.4.