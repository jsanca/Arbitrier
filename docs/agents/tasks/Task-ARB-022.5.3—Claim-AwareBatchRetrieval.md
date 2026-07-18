Task: ARB-022.5.3 — Claim-Aware Batch Retrieval

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

Do not commit changes.

If incomplete at the hard stop, create:

docs/agents/checkpoints/CHECKPOINT-ARB-022.5.3.md

and stop.

--------------------------------------------------

Context

ARB-022.5.1 introduced:

- PublishStatus.CLAIMED
- claimedBy
- claimedAt
- explicit claim lifecycle invariants

ARB-022.5.2 introduced an atomic single-event claim operation.

The runtime still retrieves messages using:

findPending(limit)

and therefore does not yet establish ownership before returning work to a
polling worker.

This slice introduces bounded, claim-aware retrieval.

Do not modify dispatch behavior yet.

--------------------------------------------------

Goal

Allow one worker to obtain a bounded collection of Outbox events that it has
successfully claimed.

The returned collection must contain only events owned by the requesting worker.

Expected conceptual operation:

claimPending(workerId, claimedAt, limit)
↓
atomic database coordination
↓
List<OutboxEvent> in CLAIMED state

--------------------------------------------------

1. Repository Contract

Introduce a persistence-neutral operation.

Suggested direction:

List<OutboxEvent> claimPending(
String workerId,
Instant claimedAt,
int limit
)

or a similarly precise name.

Requirements:

- workerId must be non-blank;
- claimedAt must be non-null;
- negative limit rejected;
- limit == 0 returns an empty list;
- never return more than limit;
- returned events must all be CLAIMED;
- returned events must all have claimedBy == workerId;
- returned events must preserve deterministic FIFO ordering.

Do not expose JPA, SQL, page, lock or Kafka types.

--------------------------------------------------

2. Atomicity Requirement

Do not implement:

SELECT pending rows
↓
return them
↓
claim later in application code

Ownership must be established before rows are returned as work.

The database must arbitrate between competing workers.

Investigate the smallest correct PostgreSQL strategy.

Potential directions include:

- SELECT ... FOR UPDATE SKIP LOCKED inside one short transaction;
- atomic UPDATE ... RETURNING;
- bounded candidate selection plus conditional update in one database operation.

Choose according to current Spring Data/JPA capabilities and repository style.

Document the selected strategy and why it is atomic.

--------------------------------------------------

3. Transaction Boundary

Claim selection and state transition must occur inside one short database
transaction.

The transaction ends before Kafka publication begins.

Do not leave locks or a database transaction open while dispatching messages.

Expected lifecycle:

short DB transaction
↓
claim rows
↓
commit
↓
return claimed events
↓
later dispatch outside transaction

--------------------------------------------------

4. Multiple Workers

Two workers calling claimPending concurrently must not receive the same event.

Example:

Worker A:
claimPending("A", now, 10)

Worker B:
claimPending("B", now, 10)

Required result:

intersection(A results, B results) == empty

The combined results may contain up to 20 distinct events.

Do not require globally deterministic division between workers; only exclusive
ownership and bounded retrieval.

--------------------------------------------------

5. Ordering

Prefer current FIFO semantics:

occurredAt ASC

If equal timestamps require a deterministic secondary ordering for safe bounded
claiming, add:

eventId ASC

Document the decision.

Claimed rows should be returned in the same logical order used to choose them.

--------------------------------------------------

6. In-Memory Repository

Implement equivalent semantics in the in-memory repository.

It must:

- claim only PENDING events;
- preserve ordering;
- respect the limit;
- update stored state to CLAIMED;
- prevent the same event from being returned to another worker.

Use the smallest correct thread-safe mechanism.

Do not mimic SQL mechanics unnecessarily; preserve behavioral parity.

--------------------------------------------------

7. Existing APIs

Keep existing operations temporarily unless removal is necessary:

findPending()
findPending(limit)
claim(eventId, ...)

Do not migrate the polling runtime in this slice.

The old retrieval methods may be deprecated later during consolidation.

This slice adds the new capability only.

--------------------------------------------------

8. Tests

Add focused tests covering:

- empty repository;
- one pending message claimed;
- multiple pending messages claimed in FIFO order;
- limit respected;
- limit == 0;
- negative limit rejected;
- blank workerId rejected;
- null claimedAt rejected;
- PUBLISHED excluded;
- FAILED excluded;
- already CLAIMED excluded;
- returned claim metadata is correct;
- a second worker cannot receive already claimed events;
- two concurrent workers receive disjoint result sets;
- total claimed count never exceeds available PENDING rows;
- JPA and in-memory semantics remain equivalent.

Use PostgreSQL/Testcontainers for the database concurrency guarantee where
required.

Avoid sleeps and timing-sensitive assertions.

No Kafka.
No scheduler.
No dispatch.

--------------------------------------------------

9. Architecture Constraints

Verify:

- repository owns atomic persistence coordination;
- polling services do not know SQL or locking;
- transactions end before publication;
- business bounded contexts remain unaware of claims;
- no retry or lease semantics are introduced.

--------------------------------------------------

10. Documentation

Create:

docs/agents/reports/ARB-022.5.3-claim-aware-batch-retrieval.md

Document:

- repository API;
- PostgreSQL claim strategy;
- transaction boundary;
- FIFO ordering;
- concurrency guarantee;
- in-memory parity;
- tests executed;
- limitations;
- intentionally deferred polling migration and claim recovery.

--------------------------------------------------

Out of Scope

Do NOT implement:

- polling-service migration
- claimed-message dispatch
- worker identity generation
- claim expiration
- abandoned-claim recovery
- retries
- backoff
- DEAD state
- scheduler changes
- metrics or tracing
- Kafka changes
- commits

--------------------------------------------------

Acceptance Criteria

✓ Bounded claim-aware retrieval exists.

✓ Returned events are already CLAIMED by the requesting worker.

✓ Concurrent workers receive disjoint event sets.

✓ Database transaction ends before dispatch.

✓ Ordering and limits are deterministic.

✓ JPA and in-memory implementations have behavioral parity.

✓ Focused concurrency tests pass.

✓ Completion report exists.

Do not begin ARB-022.5.4.