---
name: engineering-reporting
description: Create, review, reconcile, or recover Arbitrier engineering work with durable implementation, review, fix, checkpoint, and documentation-audit records. Use for every non-trivial implementation, review, recovery, architecture, security, or documentation task.
---

# Engineering Reporting Protocol

Use this protocol with the [execution-timebox skill](../execution-timebox/SKILL.md). Read [documentation ownership](../../../docs/engineering/documentation-ownership.md) when a report affects current system documentation.

## Workflow

1. Locate the task in `docs/agents/tasks/<task-id>-<slug>.md`; create it there when absent.
2. Read `docs/agents/checkpoints/CHECKPOINT-<task-id>.md` if it is OPEN. Do not report DONE until it is RESOLVED or SUPERSEDED.
3. Use the matching template in `docs/agents/templates/`, link related durable records, and add the artifact to `ENGINEERING_LOG.md`.
4. Update canonical documentation only when repository evidence supports the current-state claim. Preserve historical reports; annotate or supersede rather than silently rewriting history.

## Report Types and Locations

| Type | Required when | Canonical location |
|---|---|---|
| Implementation report | A non-trivial task completes | `docs/agents/reports/<task-id>-<slug>.md` |
| Review report | Evaluating an artifact without changing it | `docs/agents/reviews/<task-id>-review-<reviewer-role>.md` |
| Fix report | Resolving or formally deferring findings | `docs/agents/reports/<task-id>-fix-<sequence>.md` |
| Recovery checkpoint | Hard/early stop before completion | `docs/agents/checkpoints/CHECKPOINT-<task-id>.md` |
| Documentation audit | Reconciling implementation and active documentation | `docs/agents/reviews/<task-id>-documentation-audit.md` |

Architecture/security reviews reuse the review structure and add their specialized taxonomy from existing conventions.

## Mandatory Sections

- **Implementation:** Context, Summary, Deliverables, Architectural Decisions, Implementation Notes, Validation, Tests, Tradeoffs, Open Questions, Follow-ups, References.
- **Review:** Review Scope, Verdict (`PASS`, `PASS WITH WARNINGS`, `FAIL`), Findings (ID, severity, category, evidence, impact, recommendation, blocker, fix/defer), Positive Findings, Deferred Findings, Conclusion, References.
- **Fix:** Source Findings, Changes Applied, Tests and Validation, Deferred Findings, Remaining Risk, References.
- **Checkpoint:** Original Objective, Completed Work, Files Changed, Current Repository State, Validation Status, Current Blocker, Evidence, Remaining Work, Proposed Continuation Tasks, Recommended Next Action, Checkpoint Status (`OPEN`, `RESOLVED`, `SUPERSEDED`).
- **Documentation audit:** Documents Reviewed, Inconsistencies Found, Corrections Made, Stale Terminology Removed, Broken Links, Remaining Documentation Debt, Recommendations.

## Evidence Rules

- Report only commands actually run and their actual result; never claim all tests pass without evidence.
- Do not claim files, behavior, ownership, decisions, or completion without repository evidence.
- State skipped or unavailable validation explicitly.
- Keep future behavior in roadmaps, planned tasks, or open questions; active canonical documentation describes present behavior.
- Never use an implementation report as a checkpoint, store checkpoints outside the canonical directory, or mark DONE with an OPEN checkpoint.
