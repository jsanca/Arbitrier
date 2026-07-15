Task: ARB-022.3-REVIEW — Pending Polling Runtime Review

Status:
[PLANNED]

Owner:
Deep

Role:
Architecture Review

Apply:

.claude/skills/engineering-reporting/SKILL.md

Do not implement fixes.

--------------------------------------------------

Context

The following slices are complete:

ARB-022.3.1
Pending Message Retrieval

ARB-022.3.2
Sequential Pending Message Dispatch

ARB-022.3.3
Pending Message Polling Facade

Together they implement one bounded, manually invocable polling cycle:

OutboxPollingService.pollOnce()
↓
SequentialPendingDispatchService.dispatchPending(batchSize)
↓
OutboxRepository.findPending(batchSize)
↓
DispatchOutboxMessageService.dispatch(event)
↓
sequential acknowledgement and Outbox state update

No scheduler, concurrency, retry, claim semantics, or Avro transport has been
introduced.

--------------------------------------------------

Review Goal

Determine whether ARB-022.3 is a correct and durable single-worker polling
foundation and whether it is ready to be invoked by a scheduler.

--------------------------------------------------

1. Repository Retrieval

Review:

OutboxRepository.findPending(int limit)

Evaluate:

- persistence-neutral contract;
- negative and zero limit semantics;
- exclusion of PUBLISHED and FAILED rows;
- SQL-level limiting;
- parity between JPA and in-memory adapters;
- whether retaining parameterless findPending() creates ambiguity or debt.

--------------------------------------------------

2. Ordering

Current order:

occurredAt ascending

Evaluate:

- whether occurredAt is the correct FIFO field;
- behavior when timestamps are equal;
- deterministic ordering across repeated queries;
- whether a secondary eventId ordering should be added;
- whether persistedAt / createdAt would be more accurate in the future.

Recommend whether ordering needs a fix before scheduling.

--------------------------------------------------

3. Sequential Dispatch

Review the stage fold:

CompletionStage<Void> chain = completedFuture(null);

for (OutboxEvent event : pending) {
chain = chain.thenCompose(ignored -> dispatcher.dispatch(event));
}

Evaluate:

- strict sequential execution;
- ordering preservation;
- first-failure behavior;
- immediate exception behavior inside thenCompose;
- exception wrapping;
- memory/call-chain implications for configured batch sizes;
- whether stop-on-first-failure is the right initial policy.

--------------------------------------------------

4. Polling Facade

Review:

OutboxPollingService.pollOnce()

Evaluate:

- one-cycle semantics;
- constructor batch-size validation;
- direct delegation;
- framework neutrality;
- absence of repository and Kafka dependencies;
- whether the class is meaningful or unnecessary indirection.

Do not reject the facade merely because it is small; evaluate the scheduling
boundary it creates.

--------------------------------------------------

5. State Integrity

Review the interaction with the prior dispatch foundation.

Evaluate:

- current lack of PENDING guard in DispatchOutboxMessageService;
- possibility of duplicate publication;
- already-PUBLISHED or FAILED input;
- markPublished failure after Kafka acknowledgement;
- whether these issues block a single-worker scheduled poller.

Recommend either:

- guard now;
- caller-owned invariant;
- defer until claim semantics.

--------------------------------------------------

6. Single-Worker Safety

Confirm whether this design is safe under exactly one polling worker.

Evaluate:

- overlapping poll cycles;
- repeated retrieval before previous dispatch completes;
- scheduler fixed-rate vs fixed-delay implications;
- absence of CLAIMED / IN_PROGRESS;
- whether one JVM scheduler can still overlap asynchronously.

This is important: pollOnce() returns immediately with a CompletionStage, so a
scheduler may trigger another cycle before the previous stage completes.

Identify the minimum overlap-prevention requirement for ARB-022.4.

--------------------------------------------------

7. Concurrent Worker Readiness

Evaluate what must exist before:

- multiple scheduler threads;
- multiple application replicas;
- concurrent pollers.

Review the need for:

- claim semantics;
- IN_PROGRESS / CLAIMED status;
- atomic conditional update;
- SELECT FOR UPDATE SKIP LOCKED;
- leases or claim expiration;
- optimistic locking.

Do not implement these mechanisms.

--------------------------------------------------

8. Failure Policy

Current policy:

- stop batch at first failure;
- failed event is marked FAILED;
- later pending events remain untouched.

Evaluate whether this causes head-of-line blocking across cycles.

Clarify whether FAILED rows are permanently excluded and therefore require:

- retries;
- manual recovery;
- dead-message workflow.

Distinguish current correctness from future operational completeness.

--------------------------------------------------

9. Transaction Boundaries

Confirm:

- retrieval is a short repository operation;
- no database transaction remains open during sequential Kafka publication;
- status updates remain short;
- no transaction should wrap the full polling cycle.

Flag any mismatch with the project's transaction ownership rules.

--------------------------------------------------

10. Tests

Review whether tests adequately cover:

- retrieval limits;
- status exclusion;
- ordering;
- zero/negative limits;
- sequential invocation;
- first failure stops later messages;
- facade delegation;
- immediate and async failure propagation.

Identify only high-value gaps.

--------------------------------------------------

11. Readiness for Scheduler

Conclude what ARB-022.4 must own.

At minimum consider:

- property binding for batch size;
- scheduling interval;
- fixed delay vs fixed rate;
- logging of failed cycles;
- overlap prevention;
- conditional activation;
- graceful shutdown behavior.

Do not design the full scheduler implementation unless needed to explain a
finding.

--------------------------------------------------

Output

Provide:

PASS

PASS WITH WARNINGS

FAIL

For every finding include:

- ID
- severity
- category
- evidence
- impact
- recommendation
- roadmap blocker?
- fix now or future slice?

Conclude explicitly:

1. Can ARB-022.3 be marked DONE?
2. Is occurredAt ordering sufficiently deterministic?
3. Is stop-on-first-failure correct for the first runtime?
4. Is the design safe for one scheduled worker?
5. What overlap protection is required before ARB-022.4?
6. Which findings block multiple replicas?
7. Is the architecture ready for the scheduler slice?

Do not implement fixes.
Do not begin ARB-022.4.