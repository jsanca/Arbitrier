Task: ARB-022.3.1 — Pending Message Retrieval

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

If the hard stop is reached:

Create:

docs/agents/checkpoints/CHECKPOINT-ARB-022.3.1.md

and stop.

--------------------------------------------------

Context

ARB-021 introduced the Outbox persistence model.

ARB-022.2 completed the outbound dispatch foundation.

The next milestone is the polling runtime.

Before messages can be dispatched automatically, the runtime needs a
well-defined mechanism for retrieving pending messages from the Outbox.

This slice introduces retrieval only.

No dispatch.

No scheduler.

No retries.

--------------------------------------------------

Goal

Extend OutboxRepository with the ability to retrieve pending messages in a
deterministic order.

The retrieval API becomes the foundation for future polling.

--------------------------------------------------

1. Repository Contract

Introduce a repository operation for pending messages.

Suggested direction:

findPending(int limit)

or

findPendingMessages(int limit)

Return:

List<OutboxEvent>

Reject negative limits.

A limit of zero returns an empty list.

The repository contract must remain persistence-neutral.

--------------------------------------------------

2. Ordering

Messages must be returned in deterministic order.

Use the existing persisted ordering information.

Prefer:

createdAt ascending

If another ordering field is already the canonical persistence ordering,
reuse it and document the decision.

The same repository call should always return the same order for identical
repository state.

--------------------------------------------------

3. Limit Semantics

The repository must never return more than the requested limit.

Examples:

limit = 1

↓

at most one message

limit = 100

↓

at most one hundred messages

No pagination is required.

--------------------------------------------------

4. Pending Definition

Only messages currently eligible for dispatch should be returned.

Use the existing Outbox status model.

Do not introduce:

- CLAIMED
- IN_PROGRESS
- RETRYING
- NEXT_ATTEMPT

Those belong to future runtime slices.

--------------------------------------------------

5. Persistence Adapter

Implement the repository operation in the JPA adapter.

Reuse existing mapping infrastructure.

Avoid introducing custom native SQL unless genuinely required.

Keep the query simple and readable.

--------------------------------------------------

6. Tests

Add focused tests covering:

- empty repository
- one pending message
- multiple pending messages
- ordering
- limit = 1
- limit larger than available messages
- limit = 0
- negative limit rejected
- published messages excluded
- failed messages excluded (if current model requires it)

No scheduler.

No Kafka.

No dispatch.

--------------------------------------------------

7. Architecture

Verify:

- repository interface remains persistence-neutral;
- application layer does not know JPA;
- retrieval semantics are documented;
- no polling logic leaks into the repository.

--------------------------------------------------

8. Documentation

Create:

docs/agents/reports/ARB-022.3.1-pending-message-retrieval.md

Document:

- repository API
- ordering decision
- pending definition
- limit semantics
- persistence implementation
- tests executed

--------------------------------------------------

Out of Scope

Do NOT implement:

- dispatch
- scheduler
- polling service
- retries
- claim semantics
- optimistic locking
- concurrent workers
- batching
- metrics
- Kafka
- Avro

--------------------------------------------------

Acceptance Criteria

✓ Repository exposes findPending(limit).

✓ Ordering is deterministic.

✓ Limit is respected.

✓ Only pending messages are returned.

✓ Persistence adapter implemented.

✓ Focused tests pass.

✓ Completion report exists.

Do not begin ARB-022.3.2.