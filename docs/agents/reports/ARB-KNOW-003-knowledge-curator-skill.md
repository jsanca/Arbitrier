# Report: ARB-KNOW-003 — Knowledge Curator Skill

## Context

- **Owner / role:** Elito / Knowledge Curator
- **Execution status:** Complete
- **Scope:** Reusable curation skill, its concise project-governance pointer,
  and this durable report.
- **Out of scope:** Creating a knowledge base, migrating documentation,
  auditing the repository, application/infrastructure changes, and ARB-KNOW-004.
- **Timebox result:** Completed within the target window; no checkpoint required.

## Summary

Created a generic `knowledge-curator` skill that guides evidence-backed
knowledge curation without making Arbitrier's directory layout or domain model
a prerequisite. The project governance document now links to the skill rather
than duplicating its operational workflow.

## Deliverables

- `.claude/skills/knowledge-curator/SKILL.md` — reusable role mission,
  activation terms, modes, durability test, authority rules, checklist,
  boundaries, and reporting integration.
- `.claude/skills/knowledge-curator/agents/openai.yaml` — generated UI metadata.
- `docs/engineering/knowledge-curator.md` — concise pointer to the reusable
  skill; retains project-specific role governance.
- This report.

## Architectural Decisions

| Decision | Rationale |
| --- | --- |
| Use audit and reconciliation modes | Prevents an inspection task from becoming an unauthorized rewrite. |
| Treat knowledge as more stable than implementation | Keeps canonical knowledge focused on durable concepts and contracts. |
| Keep layout examples conditional | Allows the skill to work across repositories with different documentation conventions. |
| Link, do not duplicate, reporting and timebox skills | Preserves one operational authority for each protocol. |

## Implementation Notes

The skill activates for Knowledge Curator, Curator, Kurator, documentation
curation, canonical knowledge, source-of-truth validation, consistency audits,
and knowledge-base maintenance. Its mission is: “Curate. Never invent.” It
requires evidence-backed current-state claims, explicit authority links, and
preservation of historical delivery evidence.

Project-specific guidance—including the Arbitrier role definition and durable
artifact locations—remains in `docs/engineering/knowledge-curator.md` and the
existing reporting protocol rather than the reusable core.

## Validation

- Initialized the skill with the bundled skill-creator initializer.
- Attempted the bundled skill validator; it could not run because its Python
  environment lacks the `yaml` module (`ModuleNotFoundError: No module named
  'yaml'`).
- Performed manual validation: confirmed YAML frontmatter contains matching
  `name` and non-empty `description`, activation language includes
  Curator/Kurator and curation tasks, and linked skills resolve.
- Searched the reusable core for Arbitrier-specific domain terms.
- Ran `git diff --check`.
- Confirmed no application or infrastructure source files were changed by this task.

## Tests

No application tests were run because the task changes skill and documentation
artifacts only.

## Tradeoffs

The skill does not prescribe a knowledge-base taxonomy or introduce tooling for
link validation. Those are project-specific mechanics and remain separate from
the reusable curator behavior.

## Open Questions

None.

## Follow-ups

- Apply the skill to future curation audits or explicitly authorized
  reconciliation work.
- Create a separate knowledge-base skill if taxonomy mechanics become reusable.

## References

- Task specification supplied in the requesting session
- [Knowledge Curator skill](../../../.claude/skills/knowledge-curator/SKILL.md)
- [Engineering reporting](../../../.claude/skills/engineering-reporting/SKILL.md)
- [Execution timebox](../../../.claude/skills/execution-timebox/SKILL.md)
- [Project governance](../../engineering/knowledge-curator.md)
