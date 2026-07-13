Task: AGENT-SKILL-001 — Execution Timebox and Recovery Checkpoint

Status:
[PLANNED]

Owner:
Clio

Goal:

Create a reusable Claude skill under:

.claude/skills/execution-timebox/

The skill must prevent long-running agent loops and require a structured recovery checkpoint when execution exceeds the agreed timebox or stops making meaningful progress.

This is a workflow/tooling task only.

Do not modify application code.

--------------------------------------------------

Background

Recent advanced slices exceeded one hour, entered loops, consumed the full context window, and produced unreliable recaps.

We need an explicit execution policy for agent tasks.

The skill must distinguish:

- roadmap slice
- agent execution task

A roadmap slice may be large.

An agent execution task should normally complete within 20–30 minutes.

--------------------------------------------------

Execution Policy

Target duration:

20–30 minutes

Warning threshold:

30 minutes

Hard stop:

45 minutes

At 30 minutes:

- evaluate whether there is clear, measurable progress;
- if the remaining work is small and understood, continue;
- otherwise prepare to stop and split.

At 45 minutes:

- STOP implementation;
- do not continue coding;
- do not keep retrying the same build or fix;
- do not consume more context attempting to finish;
- produce a recovery checkpoint.

Also stop before 45 minutes if:

- the same failure repeats without new evidence;
- the agent begins repeating summaries or commands;
- the task objective becomes unclear;
- the implementation drifts into unrelated scope;
- the repository state can no longer be explained confidently.

--------------------------------------------------

Recovery Checkpoint Format

The checkpoint must contain exactly these sections:

1. Original Objective

What the task was supposed to deliver.

2. Completed Work

Concrete functionality completed.

3. Files Changed

Created, modified, or deleted files.

4. Current Repository State

- compiles / does not compile
- clean / partially implemented / inconsistent
- safe to continue / requires rollback

5. Validation Status

- tests executed
- tests passing
- tests failing
- tests not run
- build command and result

6. Current Blocker

The exact technical or process problem preventing completion.

7. Evidence

Relevant error messages, failing tests, or observed behavior.

Do not paste excessive logs.

8. Remaining Work

Specific unfinished items.

9. Proposed Continuation Tasks

Split remaining work into tasks sized for 15–30 minutes each.

10. Recommended Next Action

Choose one:

- continue in a new session
- assign a smaller task
- request architectural clarification
- ask another agent to review/recover
- rollback partial work

--------------------------------------------------

Task Design Guidance

The skill should advise agents to reject or split tasks that combine too many concerns.

Examples of concerns that should often be separated:

- domain model
- JPA persistence
- Flyway migration
- Kafka integration
- REST
- tests
- documentation
- infrastructure

A single agent execution task should ideally:

- have one primary outcome;
- touch a limited number of modules;
- produce one coherent commit;
- be reviewable in under 15 minutes;
- be executable in 20–30 minutes.

--------------------------------------------------

Context Drift Detection

The skill must identify execution as invalid if:

- the final report names a different task than the active task;
- repeated content appears in the final report;
- the agent cannot state what remains;
- the recap claims success without matching repository evidence.

In those cases:

- mark the execution as INCOMPLETE or INVALID;
- do not claim DONE;
- produce the recovery checkpoint instead.

--------------------------------------------------

Suggested Skill Files

Create at minimum:

.claude/skills/execution-timebox/SKILL.md

Optional supporting files are allowed if useful, but keep the skill small.

SKILL.md should contain:

- purpose
- trigger conditions
- timebox policy
- stop conditions
- recovery checkpoint template
- task-splitting guidance
- invalid execution criteria
- short examples

--------------------------------------------------

Integration

Update:

- AGENTS.md
- CLAUDE.md

Add a concise rule that all implementation agents must apply the execution-timebox skill to non-trivial tasks.

Do not duplicate the full skill contents in those files; link or reference the skill.

--------------------------------------------------

Documentation

Create:

docs/engineering/agent-execution-timebox.md

Document:

- why the policy exists;
- target and hard limits;
- recovery checkpoint behavior;
- distinction between roadmap slice and execution task;
- one anonymized example based on a long-running loop incident.

Do not mention private model internals or vendor-specific token counts.

--------------------------------------------------

Tests / Validation

No application tests required.

Validate:

- skill path and markdown structure;
- links from AGENTS.md and CLAUDE.md resolve;
- markdown links in repository remain valid;
- git diff --check passes.

--------------------------------------------------

Out of Scope

- No application code changes.
- No ARB-022 implementation.
- No automation scheduler.
- No runtime process enforcement.
- No agent benchmarking framework.
- No model-specific scoring.
- No repository rollback.

--------------------------------------------------

Acceptance Criteria

- execution-timebox skill exists under .claude/skills/
- 20–30 minute target is documented
- 30-minute warning behavior is documented
- 45-minute hard stop is mandatory
- no-progress stop conditions are included
- recovery checkpoint format is explicit
- context drift invalidation is included
- task-splitting guidance is included
- AGENTS.md and CLAUDE.md reference the skill
- engineering documentation is added
- repository-local markdown links resolve
- no application code changed

After completion:

Report:

- files created/modified
- exact hard-stop rule
- recovery checkpoint sections
- validation performed

Do not begin ARB-022.