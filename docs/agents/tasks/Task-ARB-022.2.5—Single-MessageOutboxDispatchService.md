Task: ARB-022.2.5 — Single-Message Outbox Dispatch Service

Status:
[PLANNED]

Owner:
Clio

Role:
Implementation

Timebox:
Target: 20–30 minutes
Hard stop: 45 minutes

Apply:

.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

If incomplete at the hard stop, create:

docs/agents/checkpoints/CHECKPOINT-ARB-022.2.5.md

and stop.

--------------------------------------------------

Context

ARB-021 introduced:

- OutboxEvent
- OutboxRepository
- markPublished(...)
- markFailed(...)

ARB-022.2.1 introduced:

OutboundMessagePublisher

ARB-022.2.2 introduced:

KafkaOutboundMessagePublisher

ARB-022.2.3 introduced:

OutboundPayloadSerializer

ARB-022.2.4 evolved the publisher contract to:

CompletionStage<Void> publish(OutboxEvent message)

The next step is to coordinate publication outcome with Outbox state.

This slice handles exactly one OutboxEvent.

Do not implement polling, batching, scheduling, or retries.

--------------------------------------------------

Goal

Create an application-level service that dispatches one pending OutboxEvent
through OutboundMessagePublisher and updates the Outbox state according to the
asynchronous publication result.

Expected flow:

OutboxEvent
↓
publisher.publish(event)
↓
success
↓
outboxRepository.markPublished(eventId)

or

OutboxEvent
↓
publisher.publish(event)
↓
failure
↓
outboxRepository.markFailed(eventId)

--------------------------------------------------

1. Service

Create a focused service.

Suggested name:

DispatchOutboxMessageService

or:

PublishPendingOutboxMessageService

Prefer the name that best describes dispatching one persisted message.

Suggested package direction:

com.arbitrier.platform.messaging.outbox.application

or the closest existing application-service package convention.

The service must depend only on:

- OutboundMessagePublisher
- OutboxRepository

and platform messaging types.

No Kafka imports.

--------------------------------------------------

2. Public API

Suggested contract:

CompletionStage<Void> dispatch(OutboxEvent message)

Requirements:

- reject null message;
- call publisher.publish(message) exactly once;
- return a CompletionStage representing the full workflow;
- complete normally only after markPublished succeeds;
- complete exceptionally if publication fails;
- invoke markFailed when publication fails.

Do not block.

Do not call get() or join().

--------------------------------------------------

3. Success Semantics

On successful publication:

publisher.publish(message)
↓
outboxRepository.markPublished(message.eventId())

The returned stage should complete normally only after markPublished finishes.

If markPublished throws, the returned stage must complete exceptionally.

Do not call markFailed after a markPublished persistence failure unless there is
an explicit existing repository convention requiring it.

Treat publication success + persistence failure as an unresolved operational
error for future retry/recovery handling.

--------------------------------------------------

4. Failure Semantics

If publisher.publish(message) completes exceptionally:

- call outboxRepository.markFailed(message.eventId());
- preserve the original publication failure as the primary error;
- do not swallow the exception;
- do not complete normally.

If markFailed also throws:

- preserve the publication exception;
- attach the markFailed exception as suppressed where practical;
- complete exceptionally.

Do not invent retry behavior.

--------------------------------------------------

5. Immediate Failures

If publisher.publish(message) throws before returning a CompletionStage:

- call markFailed(message.eventId());
- rethrow the original exception;
- attach markFailed failure as suppressed if needed.

This keeps synchronous validation/configuration failures consistent with
asynchronous transport failures.

--------------------------------------------------

6. Repository State

Use existing OutboxRepository methods.

Do not add new statuses.

Do not change the Outbox schema.

Do not implement:

- claiming/locking;
- in-progress status;
- retry counters;
- next-attempt timestamps.

Those belong to later slices.

--------------------------------------------------

7. Transaction Boundary

Do not place a long-running database transaction around asynchronous publication.

The service should coordinate:

- transport acknowledgement;
- short repository status updates.

If @Transactional is considered, document why it is intentionally omitted or
limited.

No database transaction may remain open while waiting for Kafka acknowledgement.

--------------------------------------------------

8. Tests

Add focused unit tests covering:

- null message is rejected;
- publisher called exactly once;
- successful publication calls markPublished;
- successful publication does not call markFailed;
- asynchronous publication failure calls markFailed;
- asynchronous publication failure remains exceptional;
- immediate publication failure calls markFailed;
- markFailed failure is attached as suppressed to publication failure;
- markPublished failure completes exceptionally;
- no blocking behavior is introduced.

Use completed and failed CompletableFuture instances.

No Kafka broker.
No Docker.
No Testcontainers.

--------------------------------------------------

9. Architecture

Verify:

- no Kafka types in the dispatch service;
- service depends on ports, not adapters;
- publisher and repository remain independently replaceable;
- service handles one message only.

Add no new framework dependency unless genuinely required.

--------------------------------------------------

10. Documentation

Create:

docs/agents/reports/ARB-022.2.5-single-message-outbox-dispatch-service.md

Document:

- service name and package;
- success flow;
- failure flow;
- transaction decision;
- error-preservation behavior;
- tests executed;
- deliberately deferred concerns.

--------------------------------------------------

Out of Scope

Do NOT implement:

- findPending()
- polling
- batch dispatch
- scheduler
- distributed locking
- claim/in-progress status
- retries
- exponential backoff
- DLQ
- metrics
- tracing
- Kafka consumers
- Inbox processing
- Avro serialization
- Schema Registry
- command publisher replacement
- application-service wiring

--------------------------------------------------

Acceptance Criteria

✓ One-message dispatch service exists.

✓ Publication acknowledgement drives Outbox status.

✓ markPublished occurs only after publication success.

✓ markFailed occurs on synchronous and asynchronous publication failure.

✓ Original publication errors are preserved.

✓ No transaction remains open while awaiting Kafka.

✓ No polling, scheduling, batching, or retry logic is introduced.

✓ Focused unit tests pass.

✓ Completion report exists.

Do not begin ARB-022.2.6.