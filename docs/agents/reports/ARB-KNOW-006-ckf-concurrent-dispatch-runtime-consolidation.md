# Report: ARB-KNOW-006 — CKF Concurrent Dispatch Runtime Consolidation

## Context

- **Owner / role:** Elito / Knowledge Curator
- **Execution status:** Complete
- **Scope:** Consolidate durable ARB-022.5 claim-based dispatch knowledge,
  reconcile Knowledge Base navigation, record ARB-022 status, and preserve the
  existing implementation reports as authority.
- **Out of scope:** Application changes, retry/backoff design, stale-claim
  recovery, metrics, runtime tuning, and historical-report rewrites.
- **Timebox result:** Completed within the target window; no checkpoint required.

## Summary

Created one canonical knowledge page for the concurrent dispatch runtime rather
than reproducing four implementation reports. It captures the claim lifecycle,
worker ownership, database-versus-transport boundary, concurrent batch
acquisition, `SKIP LOCKED`, transaction boundary, polling behavior, and the
current recovery limitation.

## Deliverables

- `knowledge/operations/concurrent-dispatch-runtime.md` — canonical conceptual
  consolidation of ARB-022.5.
- `knowledge/operations/index.md` — Operations navigation and authority update.
- `knowledge/architecture/index.md` — cross-reference to the dispatch boundary.
- `knowledge/index.md` — Operations navigation wording update.
- `ENGINEERING_LOG.md` — aggregate ARB-022 status and ARB-KNOW-006 delivery row.
- This task record and report.

## Architectural Decisions

| Decision | Rationale |
| --- | --- |
| Use one Operations page | The listed concepts form one lifecycle and would be duplicated across separate pages. |
| Link historical reports as authority | The knowledge page curates stable concepts; it does not replace implementation evidence. |
| Keep ARB-022 IN PROGRESS | ARB-022.5 is complete, while stale-claim recovery and retry/backoff remain deferred to ARB-022.6. |
| Cross-link Architecture rather than duplicate text | Database ownership and transport separation are architectural relationships with one canonical explanation. |

## Implementation Notes

No existing Knowledge Base page described outbox claims, so no duplicate page
was removed. The new page is the appropriate CKF candidate because its concepts
remain useful across refactoring: claim lifecycle, ownership, transaction
boundary, dispatch exclusivity, and recovery limits.

## Validation

- Reviewed ARB-022.5.1 through ARB-022.5.4 reports, ADR-0005, Platform source,
  and current Engineering Log status.
- Verified repository-local Markdown links and Knowledge Base reachability.
- Checked the knowledge page for implementation class names, method names,
  package names, framework annotations, and duplicated authority.
- Ran `git diff --check`.
- Confirmed no application or infrastructure source files were changed by this task.

## Tests

No application tests were run because this task changes documentation and
knowledge-navigation artifacts only. The authoritative ARB-022.5 reports record
their implementation validation separately.

## Tradeoffs

The page states that PostgreSQL `SKIP LOCKED` is the current coordination
pattern because it is a durable operational constraint. It does not expose SQL,
repository APIs, implementation classes, or method-level behavior.

## Open Questions

- Stale-claim recovery, retry, backoff, and retry scheduling remain deferred to
  ARB-022.6.

## Follow-ups

- Add a recovery and retry knowledge page only after ARB-022.6 produces
  authoritative behavior.
- Add delivery observability knowledge when metrics and tracing are implemented.

## References

- [Task](../tasks/ARB-KNOW-006%E2%80%94CKF-ConcurrentDispatchRuntimeConsolidation.md)
- [Concurrent Dispatch Runtime](../../../knowledge/operations/concurrent-dispatch-runtime.md)
- [ARB-022.5.1](ARB-022.5.1-outbox-claim-state-model.md)
- [ARB-022.5.2](ARB-022.5.2-atomic-claim-repository.md)
- [ARB-022.5.3](ARB-022.5.3-claim-aware-batch-retrieval.md)
- [ARB-022.5.4](ARB-022.5.4-multi-worker-claim-based-polling.md)
