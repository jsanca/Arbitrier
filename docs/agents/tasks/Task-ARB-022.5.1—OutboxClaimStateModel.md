Task: ARB-022.5.1 — Outbox Claim State Model

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

docs/agents/checkpoints/CHECKPOINT-ARB-022.5.1.md

and stop.

--------------------------------------------------

Context

ARB-022.4 completed the canonical single-instance polling runtime:

Spring Scheduler
↓
OutboxPollingService
↓
SequentialPendingDispatchService
↓
DispatchOutboxMessageService
↓
OutboundMessagePublisher

The current runtime prevents overlapping cycles inside one JVM through an
AtomicBoolean.

That protection does not prevent two application replicas from reading and
publishing the same PENDING OutboxEvent.

ARB-022.5 introduces distributed claim semantics.

This first slice defines the durable claim state and its invariants only.

Do not implement atomic claiming or concurrent workers yet.

--------------------------------------------------

Goal

Extend the Outbox state model so a message can be temporarily owned by one
worker before publication.

The model must distinguish:

PENDING
→ eligible to be claimed

CLAIMED
→ temporarily owned by one worker

PUBLISHED
→ successfully delivered

FAILED
→ publication attempt failed under the current pre-retry model

This slice introduces claim representation and legal state transitions.

--------------------------------------------------

1. Publish Status

Add:

CLAIMED

to the existing Outbox publish-status model.

Review all exhaustive switches, validation, persistence mapping and database
constraints affected by the new value.

Do not add:

- RETRYING
- RETRY_WAIT
- DEAD
- EXPIRED

Those belong to ARB-022.6 or ARB-022.5.5.

--------------------------------------------------

2. Claim Identity

Extend OutboxEvent with the minimum durable claim information.

Suggested fields:

String claimedBy
Instant claimedAt

Prefer an existing WorkerId / InstanceId value object only if one already exists
and clearly fits.

Do not create a broad distributed-worker abstraction in this slice.

Semantics:

- PENDING:
  claimedBy == null
  claimedAt == null

- CLAIMED:
  claimedBy is non-blank
  claimedAt is non-null

- PUBLISHED:
  claim metadata is cleared

- FAILED:
  claim metadata is cleared

Do not add claimExpiresAt yet unless the current model cannot represent recovery
without it. Claim expiration belongs to ARB-022.5.5.

--------------------------------------------------

3. Domain Transitions

Introduce explicit state transitions on OutboxEvent or its established
lifecycle owner.

Suggested semantics:

claim(workerId, claimedAt)

Requirements:

- only PENDING may become CLAIMED;
- workerId must be non-blank;
- claimedAt must be non-null;
- claim metadata is recorded;
- repeated claim of CLAIMED is rejected;
- terminal states cannot be claimed.

markPublished(...)

Requirements:

- CLAIMED may become PUBLISHED;
- claim metadata is cleared.

markFailed(...)

Requirements:

- CLAIMED may become FAILED;
- claim metadata is cleared.

Review whether direct transitions from PENDING to PUBLISHED / FAILED are still
needed by current tests or compatibility paths.

Prefer a deliberate migration path rather than silently preserving invalid
lifecycle shortcuts.

Do not broadly redesign the existing Outbox lifecycle.

--------------------------------------------------

4. Invariants

Centralize and test at least these invariants:

PENDING
- no claim owner
- no claim timestamp

CLAIMED
- claim owner required
- claim timestamp required

PUBLISHED
- no active claim metadata

FAILED
- no active claim metadata

Invalid persisted combinations must fail during model construction or mapping
according to current platform conventions.

Do not silently normalize corrupt persisted state.

--------------------------------------------------

5. Persistence Schema

Add the minimum Flyway migration required for:

- CLAIMED status in the publish_status CHECK constraint;
- nullable claimed_by;
- nullable claimed_at.

Suggested column direction:

claimed_by VARCHAR(255)
claimed_at TIMESTAMPTZ

Do not add:

- worker heartbeat
- lease duration
- claim_expires_at
- retry counters
- next_attempt_at
- dead-letter fields

Name the migration according to the existing per-schema/platform convention.

--------------------------------------------------

6. Persistence Mapping

Update:

- OutboxEventEntity
- JPA mapping
- record/domain conversion
- in-memory repository behavior where relevant

Ensure round-trip persistence preserves claim metadata exactly.

Do not add claim queries or modifying SQL yet.

--------------------------------------------------

7. Existing Repository Operations

Do not implement:

claim(...)
claimBatch(...)
findAndClaim(...)
SELECT FOR UPDATE
SKIP LOCKED

Those belong to ARB-022.5.2 and ARB-022.5.3.

Existing findPending(limit) must continue returning only PENDING messages.

CLAIMED messages must be excluded automatically by status filtering.

--------------------------------------------------

8. Compatibility Review

Inspect current paths that create OutboxEvent instances.

Update factories/builders/test fixtures so newly created events are:

status = PENDING
claimedBy = null
claimedAt = null

Avoid adding claim arguments to every call site when a canonical factory or
constructor default can preserve clarity.

Do not hide claim metadata behind arbitrary null positional parameters if a
clear factory method is preferable.

--------------------------------------------------

9. Tests

Add focused tests covering:

- new event starts PENDING with no claim metadata;
- PENDING event can be claimed;
- claimed event records worker and timestamp;
- CLAIMED event cannot be claimed again;
- PUBLISHED event cannot be claimed;
- FAILED event cannot be claimed;
- blank worker ID rejected;
- null claim timestamp rejected;
- CLAIMED requires both claim fields;
- PENDING rejects claim metadata;
- markPublished from CLAIMED clears claim metadata;
- markFailed from CLAIMED clears claim metadata;
- JPA round-trip preserves CLAIMED state and metadata;
- findPending excludes CLAIMED messages;
- database constraint accepts CLAIMED and rejects unknown status where current
  migration tests support this.

Keep tests focused on state and persistence.

No scheduler.
No Kafka.
No concurrent threads.
No claim query tests.

--------------------------------------------------

10. Architecture

Verify:

- claim state remains a platform Outbox concept;
- no Kafka types introduced;
- no Spring Scheduler dependency introduced;
- no worker-execution implementation leaks into the domain model;
- business bounded contexts remain unaware of claim mechanics.

--------------------------------------------------

11. Documentation

Create:

docs/agents/reports/ARB-022.5.1-outbox-claim-state-model.md

Document:

- new state;
- claim metadata;
- lifecycle transitions;
- invariants;
- schema changes;
- compatibility decisions;
- tests executed;
- intentionally deferred atomic claim and expiration concerns.

Update canonical implementation documentation only where required to keep the
current Outbox lifecycle accurate.

--------------------------------------------------

Out of Scope

Do NOT implement:

- atomic repository claim operation
- batch claiming
- SELECT FOR UPDATE SKIP LOCKED
- multiple workers
- worker IDs generated from hostname/pod
- leases
- claim expiration
- abandoned-claim recovery
- scheduler changes
- retry counters
- exponential backoff
- next-attempt scheduling
- DEAD status
- Kafka changes
- Avro changes
- metrics or tracing

--------------------------------------------------

Acceptance Criteria

✓ CLAIMED exists in the Outbox status model.

✓ Claim owner and timestamp are represented durably.

✓ Legal claim transitions are explicit.

✓ Invalid status/metadata combinations are rejected.

✓ Claim metadata is cleared on publication success or failure.

✓ Database schema and JPA mapping support CLAIMED.

✓ findPending continues to return only unclaimed PENDING events.

✓ Focused domain and persistence tests pass.

✓ Completion report exists.

Do not begin ARB-022.5.2.