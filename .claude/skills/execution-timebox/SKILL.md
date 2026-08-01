# Skill: Execution Timebox and Recovery Checkpoint

## Purpose

Prevent agent loops, context exhaustion, and unreliable recaps on non-trivial implementation tasks. When a task exceeds its timebox or stops making progress, stop cleanly and produce a structured recovery checkpoint instead of continuing to consume context.

## Trigger Conditions

Apply this skill to any **agent execution task** — a task with a concrete implementation outcome (code, migrations, tests, documentation). Do not apply to roadmap slices (which may be intentionally large and multi-session).

**Roadmap slice** — defines what to build across multiple tasks; may be large by design.

**Agent execution task** — delivers one concrete outcome; must complete in 20–30 minutes.

---

## Timebox Policy

| Threshold | Action |
|-----------|--------|
| 20–30 min | Target completion window |
| 30 min | Evaluate progress; continue only if remaining work is small and fully understood |
| 45 min | **HARD STOP** — produce recovery checkpoint; do not continue coding |

At the 30-minute mark, ask: Is there clear, measurable progress? Can I state exactly what remains and how long it will take? If yes and it is small, continue. If no, stop now.

---

## Stop Conditions (before 45 minutes)

Stop immediately if any of these are true:

- The same build failure or test failure repeats without new diagnostic evidence.
- The agent begins repeating summaries, commands, or reasoning.
- The task objective has become unclear or has shifted.
- The implementation has drifted into scope that was not in the original task.
- The repository state can no longer be explained confidently (what changed, why, does it compile).

---

## Recovery Checkpoint Format

Produce `docs/agents/checkpoints/CHECKPOINT-<task-id>.md` when a hard stop or early stop occurs. It is temporary operational memory, not an implementation report. Every section is required.

```
## Recovery Checkpoint

### 1. Original Objective
What the task was supposed to deliver.

### 2. Completed Work
Concrete functionality completed and verified.

### 3. Files Changed
Created, modified, or deleted — with one-line description of each change.

### 4. Current Repository State
- Compiles / does not compile
- Clean / partially implemented / inconsistent
- Safe to continue / requires rollback

### 5. Validation Status
- Tests executed: yes / no
- Tests passing: N / M (list failing class names)
- Build command run: `mvn -B test --no-transfer-progress -pl ...`
- Build result: PASS / FAIL / NOT RUN

### 6. Current Blocker
The exact technical or process problem preventing completion.
One paragraph. Be specific.

### 7. Evidence
Relevant error messages, failing test names, or observed behavior.
Do not paste full stack traces or long logs — excerpt only.

### 8. Remaining Work
Specific unfinished items as a checklist.

### 9. Proposed Continuation Tasks
Split remaining work into tasks sized for 15–30 minutes each.
Give each a name, scope, and estimated duration.

### 10. Recommended Next Action
Choose one:
- Continue in a new session (attach this checkpoint)
- Assign a smaller task (name it)
- Request architectural clarification (state the question)
- Ask another agent to review/recover
- Rollback partial work (explain what to undo)

### 11. Checkpoint Status
Use `OPEN`, `RESOLVED`, or `SUPERSEDED`.
```

Before continuing a task, read its existing OPEN checkpoint. Mark it RESOLVED or SUPERSEDED after recovery. A task cannot be reported DONE while an OPEN checkpoint exists. A hard-stop execution must write the checkpoint before ending; the final recap must name the active task exactly. Repeated or mismatched recaps invalidate the execution. Never store checkpoints as final implementation reports.

---

## Task-Splitting Guidance

Reject or split a task that combines too many concerns. Each concern below is a candidate for its own execution task:

- Domain model
- JPA persistence adapters and entities
- Flyway migrations
- Kafka consumers / producers
- REST controllers
- Tests (unit, integration, JPA adapter)
- Documentation
- Infrastructure / configuration

A well-scoped execution task:

- Has one primary outcome.
- Touches a limited number of modules (ideally one).
- Produces one coherent commit.
- Is reviewable in under 15 minutes.
- Is executable in 20–30 minutes.

---

## Invalid Execution Criteria

Mark an execution as **INCOMPLETE** or **INVALID** (do not claim DONE) if:

- The final report names a different task than the one that was active.
- Repeated content appears in the final report (copy-paste loops).
- The agent cannot state what work remains.
- The recap claims success but the repository does not compile or tests fail.
- The reported files changed do not match `git diff`.

Produce the recovery checkpoint in all of these cases.

---

## Short Examples

**Valid stop at 30 minutes:**

> "Persistence mapper and adapter are complete and tested. The remaining work is wiring the `@PersistenceConfiguration` — approximately 10 minutes. Continuing."

**Hard stop at 45 minutes:**

> "Hard stop. The Flyway migration has run but Hibernate validation fails on a column type mismatch. The same error has occurred three times. Producing recovery checkpoint."

**Early stop — same failure repeating:**

> "The ArchUnit test has failed four times with the same violation. I have changed the package structure twice without resolving it. Stopping to produce a checkpoint rather than consuming more context."

**Invalid execution — do not claim DONE:**

> The recap states "all tests pass" but `git diff` shows test files are not yet created. Mark INVALID. Produce checkpoint.
