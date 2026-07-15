Task: ARB-022.4.1 — Polling Cycle Overlap Prevention

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

docs/agents/checkpoints/CHECKPOINT-ARB-022.4.1.md

and stop.

--------------------------------------------------

Context

Completed:

ARB-022.3.3
OutboxPollingService

Architecture review (ARB-022.3-REVIEW) identified one HIGH finding:

A future scheduler could invoke pollOnce() while a previous polling cycle is
still executing asynchronously.

This slice addresses only that concern.

No scheduler is introduced yet.

--------------------------------------------------

Goal

Prevent overlapping polling cycles inside OutboxPollingService.

Only one polling cycle may execute at any given time.

If another caller invokes pollOnce() while a previous cycle is still active,
the second invocation should return immediately without starting a second
dispatch cycle.

--------------------------------------------------

1. Polling State

Introduce minimal runtime state inside OutboxPollingService.

Suggested implementation:

AtomicBoolean

or another equally simple thread-safe mechanism.

Avoid locks, semaphores or executors.

--------------------------------------------------

2. Behaviour

pollOnce()

If no cycle is running:

- mark cycle as active;
- invoke SequentialPendingDispatchService.dispatchPending(batchSize);
- clear the active flag when the CompletionStage completes
  (success or failure);
- return the original CompletionStage.

If a cycle is already active:

- do NOT invoke dispatchPending();
- return CompletableFuture.completedFuture(null).

This "skip" behaviour should be documented.

--------------------------------------------------

3. Completion

The running flag must always be cleared.

Including:

- successful completion;
- exceptional completion;
- immediate exception before a CompletionStage is returned.

Do not leak the running state.

--------------------------------------------------

4. Tests

Add focused tests covering:

- first poll executes;
- concurrent poll returns immediately;
- dispatcher invoked only once;
- running flag cleared after success;
- running flag cleared after failure;
- running flag cleared after immediate exception;
- polling may execute again after completion.

No scheduler.

No repository.

No Kafka.

--------------------------------------------------

5. Architecture

Verify:

- overlap prevention belongs to OutboxPollingService;
- scheduler remains unaware of running state;
- no transport concerns introduced;
- no distributed coordination introduced.

Do not implement:

- CLAIMED
- IN_PROGRESS
- leases
- distributed locks
- multi-node coordination

Those belong to ARB-022.5.

--------------------------------------------------

6. Documentation

Create:

docs/agents/reports/ARB-022.4.1-polling-cycle-overlap-prevention.md

Document:

- overlap scenario;
- chosen synchronization mechanism;
- skip policy;
- why scheduler remains simple;
- tests executed.

--------------------------------------------------

Out of Scope

Do NOT implement:

- @Scheduled
- Spring scheduling
- fixedDelay
- fixedRate
- configuration properties
- retries
- metrics
- multiple workers
- claim semantics
- distributed coordination

--------------------------------------------------

Acceptance Criteria

✓ Only one polling cycle may execute at a time.

✓ Concurrent pollOnce() invocations do not start additional dispatch cycles.

✓ Running state is always released.

✓ Scheduler remains stateless.

✓ Focused tests pass.

✓ Completion report exists.

Do not begin ARB-022.4.2.