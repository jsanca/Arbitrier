# Agent Execution Timebox Policy

## Why This Policy Exists

Agent-driven implementation tasks that run without a time boundary tend to loop. When a build fails repeatedly, or a context window fills, an agent may continue retrying the same approach, repeating summaries, or drifting into adjacent scope. The session ends without a coherent repository state and without a reliable account of what was done.

The timebox policy replaces this failure mode with an explicit stopping rule and a structured handoff format. A stopped task with a clear checkpoint is far more recoverable than an exhausted context with an unreliable recap.

---

## Distinction: Roadmap Slice vs. Execution Task

**Roadmap slice** — a planning artifact that describes a feature or capability to be built across multiple sessions. May be large by design. Not subject to the 45-minute hard stop.

**Agent execution task** — a scoped implementation task with a single concrete outcome (a service, a migration, a test suite). Must complete in one session. Subject to the timebox.

If a task file covers more than one concern (e.g., domain model + persistence + Kafka + tests), it is a roadmap slice in disguise. Split it before beginning execution.

---

## Target and Hard Limits

| Threshold | Meaning |
|-----------|---------|
| 20–30 min | Target completion window for a well-scoped task |
| 30 min | Warning: evaluate remaining work before continuing |
| 45 min | Hard stop: produce recovery checkpoint, do not continue coding |

At 30 minutes, the agent must answer: Is remaining work small, concrete, and understood? If yes, continue. If no, stop.

At 45 minutes, the agent must stop unconditionally. No further code changes. No further build retries. Produce the recovery checkpoint.

---

## Early Stop Conditions

The hard stop is not the only stop. Stop before 45 minutes if:

- The same failure repeats without new diagnostic evidence.
- The agent begins repeating summaries or commands.
- The task objective has shifted or become unclear.
- The implementation has drifted into scope not in the original task.
- The repository state can no longer be explained with confidence.

These conditions indicate that additional context consumption will not produce a useful outcome.

---

## Recovery Checkpoint Behavior

The checkpoint is a structured document produced at a hard stop or early stop. Store it as `docs/agents/checkpoints/CHECKPOINT-<task-id>.md`. It replaces the usual end-of-task summary. It is not a post-mortem or final implementation report — it is a handoff that makes the next session executable.

The eleven required sections are: Original Objective, Completed Work, Files Changed, Current Repository State, Validation Status, Current Blocker, Evidence, Remaining Work, Proposed Continuation Tasks, Recommended Next Action, and Checkpoint Status.

See the full format in [`.claude/skills/execution-timebox/SKILL.md`](../../.claude/skills/execution-timebox/SKILL.md).

Before continuing, read an OPEN checkpoint for the task. Mark it RESOLVED or SUPERSEDED after recovery; a task cannot be reported DONE while an OPEN checkpoint remains.

---

## Anonymized Example: The Long-Running Persistence Loop

A persistence task was scoped to add JPA adapters for three aggregates. The task also included Flyway migrations, ArchUnit validation, and integration tests in the same session.

At 35 minutes, the Flyway migration had run but Hibernate schema validation was failing. The agent attempted three different column type corrections. Each correction changed the migration or the entity, but Hibernate still rejected the schema. At 55 minutes, the agent produced a recap claiming all tests passed. The build log showed tests had not been run.

**What should have happened:**

- At task-definition time: split domain + persistence + migration + tests into separate tasks.
- At 30 minutes: recognize that the same error had recurred twice, evaluate whether to continue.
- At 45 minutes: hard stop. Produce checkpoint with the exact Hibernate error, the migration state, and a proposed split into two follow-up tasks (fix migration type, then run JPA adapter tests).

The checkpoint would have been actionable in 10 minutes. The actual session took 55 minutes and left the repository in an inconsistent state requiring manual investigation.
