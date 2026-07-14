Task: ARB-022.3.2 — Sequential Pending Message Dispatch

Status:
[PLANNED]

Owner:
Clio

Role:
Implementation

Timebox:
Target: 15–25 minutes

Hard stop:
45 minutes

Apply:

.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

If interrupted:

Create:

docs/agents/checkpoints/CHECKPOINT-ARB-022.3.2.md

and stop.

--------------------------------------------------

Context

Completed:

ARB-022.2.5
DispatchOutboxMessageService

ARB-022.3.1
OutboxRepository.findPending(limit)

The runtime can now:

- retrieve pending messages;
- dispatch one message.

This slice connects both capabilities.

No scheduler.

No polling loop.

No retries.

--------------------------------------------------

Goal

Create an application service that retrieves a bounded collection of pending
messages and dispatches them sequentially.

The service processes one message at a time.

No concurrency is introduced.

--------------------------------------------------

1. Service

Create:

SequentialPendingDispatchService

(or a similar name expressing sequential dispatch.)

Suggested package:

platform.messaging.outbox.application

The service depends only on:

- OutboxRepository
- DispatchOutboxMessageService

No Kafka imports.

--------------------------------------------------

2. Public API

Suggested signature:

CompletionStage<Void> dispatchPending(int limit)

Requirements:

- reject negative limits;
- limit == 0 returns an already-completed stage;
- retrieve pending messages using findPending(limit);
- dispatch every retrieved message sequentially;
- preserve repository ordering.

--------------------------------------------------

3. Sequential Composition

Messages must be dispatched strictly one after another.

Example:

message1
↓
dispatch
↓
completed
↓
message2
↓
dispatch
↓
completed
↓
...

Do not dispatch in parallel.

Do not use:

parallelStream()

CompletableFuture.allOf()

ExecutorService

Virtual Threads

Reactive streams

--------------------------------------------------

4. Failure Behaviour

If dispatch of one message fails:

- stop processing;
- complete exceptionally;
- remaining messages are NOT dispatched.

Document this decision.

Future retry policy belongs to later slices.

--------------------------------------------------

5. Empty Repository

If:

findPending(limit)

returns an empty list:

return a completed CompletionStage immediately.

Do not invoke DispatchOutboxMessageService.

--------------------------------------------------

6. Tests

Add focused tests covering:

- empty repository;
- one pending message;
- multiple pending messages;
- dispatch order preserved;
- limit respected through repository;
- first dispatch failure stops remaining dispatches;
- negative limit rejected;
- zero limit returns completed stage.

Mock:

OutboxRepository

DispatchOutboxMessageService

No Kafka.

No scheduler.

No polling timer.

--------------------------------------------------

7. Architecture

Verify:

- service depends only on ports/application services;
- sequential behaviour is explicit;
- no concurrency introduced;
- repository ordering preserved.

--------------------------------------------------

8. Documentation

Create:

docs/agents/reports/ARB-022.3.2-sequential-pending-message-dispatch.md

Document:

- service
- sequential algorithm
- failure semantics
- ordering guarantees
- tests executed

--------------------------------------------------

Out of Scope

Do NOT implement:

- scheduler

- @Scheduled

- infinite polling loop

- retries

- claim semantics

- concurrent workers

- metrics

- tracing

- Avro

- Kafka changes

--------------------------------------------------

Acceptance Criteria

✓ SequentialPendingDispatchService exists.

✓ Pending messages are retrieved once.

✓ Messages are dispatched sequentially.

✓ Ordering is preserved.

✓ First failure stops processing.

✓ Empty repository handled.

✓ Focused tests pass.

✓ Completion report exists.

Do not begin ARB-022.3.3.