Task: ARB-022.5.2 — Atomic Claim Repository

Status:
[DONE]

Owner:
Clio

Role:
Implementation

Timebox:
Target: 25–35 minutes
Hard stop: 45 minutes

Apply:

.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

If interrupted:

Create:

docs/agents/checkpoints/CHECKPOINT-ARB-022.5.2.md

and stop.

--------------------------------------------------

Context

ARB-022.5.1 introduced the durable claim model.

The runtime now understands:

PENDING

↓

CLAIMED

No concurrent acquisition mechanism exists yet.

This slice introduces only the repository operation that atomically acquires
ownership.

Do not modify polling or dispatch behavior.

--------------------------------------------------

Goal

Provide a repository operation that allows exactly one worker to transition a
PENDING OutboxEvent into CLAIMED.

The operation must be atomic at the database level.

--------------------------------------------------

1. Repository API

Introduce a focused repository operation.

Suggested direction:

claim(...)

or

claimEvent(...)

Return either:

Optional<OutboxEvent>

or

boolean

according to the current repository conventions.

Avoid returning row counts directly from higher layers.

--------------------------------------------------

2. Atomic SQL

Implement the claim using a single database update.

The transition must succeed only when:

status = PENDING

and

claimed_by IS NULL

The operation records:

status = CLAIMED

claimed_by

claimed_at

Do not use:

SELECT

↓

UPDATE

as two independent operations.

The claim must be atomic.

--------------------------------------------------

3. Concurrency

The implementation should naturally support multiple JVMs.

Two concurrent workers attempting to claim the same event must result in:

Worker A

↓

CLAIMED

Worker B

↓

claim failed

without exceptions.

--------------------------------------------------

4. Repository Responsibility

The repository owns:

atomic persistence.

The repository does NOT own:

retry

polling

dispatch

scheduler

lease expiration

worker lifecycle

--------------------------------------------------

5. Tests

Cover:

single successful claim

claim already claimed event

claim published event

claim failed event

two concurrent claim attempts

exactly one succeeds

claim metadata persisted

round-trip mapping

No Kafka.

No Scheduler.

Use database-backed tests where appropriate.

--------------------------------------------------

6. Architecture

Verify:

- no polling logic leaked
- no scheduler dependency
- no retry behavior
- no worker orchestration
- claim remains persistence concern

--------------------------------------------------

7. Documentation

Create:

docs/agents/reports/ARB-022.5.2-atomic-claim-repository.md

Document:

- repository API
- SQL strategy
- concurrency guarantees
- limitations
- tests executed

--------------------------------------------------

Out of Scope

Do NOT implement:

batch claim

claimNextBatch()

polling changes

dispatcher changes

lease expiration

retry

dead messages

metrics

observability

--------------------------------------------------

Acceptance Criteria

✓ Atomic repository claim exists.

✓ Only one worker can claim a PENDING event.

✓ Claim metadata persisted.

✓ Concurrent tests demonstrate single ownership.

✓ Completion report created.

Do not begin ARB-022.5.3.