# Report: ARB-KNOW-005 — Knowledge Base: Bounded Context Cartography

## Context

- **Owner / role:** Elito / Knowledge Curator
- **Execution status:** Complete
- **Scope:** Initial durable cartography pages for Order, Inventory, Credit,
  Orchestrator, and Platform; domain-index navigation; engineering-log entry;
  and this report.
- **Out of scope:** Diagrams, database documentation, workflows, sequence
  flows, runtime documentation, architecture redesign, and application changes.
- **Timebox result:** Completed within the target window; no checkpoint required.

## Summary

Added five evidence-backed bounded-context pages under `knowledge/domain/` and
made them reachable from the Domain index. Each page describes durable purpose,
responsibilities, exclusions, main concepts, collaboration direction,
authoritative sources, and future knowledge without exposing implementation
classes or mechanics.

## Deliverables

- `knowledge/domain/order-service.md`
- `knowledge/domain/inventory-service.md`
- `knowledge/domain/credit-service.md`
- `knowledge/domain/orchestrator-service.md`
- `knowledge/domain/platform.md`
- `knowledge/domain/index.md` — bounded-context navigation.
- `ENGINEERING_LOG.md` — durable ARB-KNOW-005 task/report index entry.
- This report.

## Architectural Decisions

| Decision | Rationale |
| --- | --- |
| Document context boundaries, not implementation structure | Keeps cartography durable through refactoring. |
| Treat Platform as a domain-neutral context | Makes its one-way relationship to business contexts explicit without assigning it business ownership. |
| Link requirements, ADRs, service guides, and contracts | Preserves each artifact's authority and prevents knowledge pages from becoming duplicate truth. |
| Defer workflows and data pages | They require separate, focused curation tasks. |

## Implementation Notes

Order owns the buyer-facing order lifecycle; Inventory owns availability,
reservation, and internal warehouse allocation; Credit owns credit reservation;
Orchestrator owns cross-context saga coordination; Platform owns domain-neutral
capabilities. The pages explicitly identify these boundaries and collaborators.

## Validation

- Verified all five context pages are reachable from `knowledge/domain/index.md`.
- Verified repository-local Markdown links in the changed knowledge pages,
  report, and log entry.
- Reviewed terminology and ownership against UC-01, ADR-0002, ADR-0003,
  ADR-0005, ADR-0009, service guides, and the contract guide.
- Checked pages for implementation-class names, package names, framework
  annotations, persistence details, and sequence-flow documentation.
- Ran `git diff --check`.
- Confirmed no application or infrastructure source files were changed by this task.

## Tests

No application tests were run because this task creates documentation-only
knowledge artifacts.

## Tradeoffs

The Main Concepts sections name stable aggregate and contract concepts but do
not attempt exhaustive domain-model coverage. This is intentional: detailed
events, workflows, tables, and operational knowledge remain future pages.

## Open Questions

None introduced. Existing authority-specific open questions remain in their
source artifacts.

## Follow-ups

- Curate aggregate/value-object pages only where cross-source navigation adds value.
- Create separate event, workflow, data, and diagram tasks when authorized.

## References

- [Task](../tasks/Task-ARB-KNOW-005%E2%80%94KnowledgeBase%3ABoundedContextCartography.md)
- [Domain Knowledge](../../../knowledge/domain/index.md)
- [Knowledge Curator skill](../../../.claude/skills/knowledge-curator/SKILL.md)
- [Engineering Log](../../../ENGINEERING_LOG.md)
