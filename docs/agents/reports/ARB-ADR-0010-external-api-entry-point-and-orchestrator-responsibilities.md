# ARB-ADR-0010 — External API Entry Point and Orchestrator Responsibilities: Implementation Report

## Context

ARB-ADR-0010 is a planned Architecture Documentation task owned by Elito. It records the UC-01 external command boundary and the Orchestrator's post-acceptance workflow responsibility. Production code and tests were explicitly out of scope; no commit was made.

## Summary

ADR-0010 establishes Order Service as the external command API for Order creation and confines the Orchestrator to the distributed workflow after Order acceptance. It explicitly rejects treating the Orchestrator as an API Gateway or BFF and defers either edge-layer pattern to a future, separately decided architecture.

## Deliverables

* `docs/adr/ADR-0010-external-api-entry-point-and-orchestrator-responsibilities.md` — planned ADR.
* `docs/agents/tasks/ARB-ADR-0010-external-api-entry-point-and-orchestrator-responsibilities.md` — durable task record.
* This report and the ARB-ADR-0010 entry in `ENGINEERING_LOG.md`.

## Architectural Decisions

* UI sends Order-creation commands to Order Service, which persists the Order and publishes the accepted-order handoff.
* The Orchestrator handles the subsequent Inventory and Credit workflow, including its saga state and compensation coordination.
* The Orchestrator has no primary REST API and is neither an API Gateway nor a BFF.
* API Gateway/BFF adoption is deferred until multiple public services or clients, UI aggregation needs, or centralized edge controls justify it.

## Implementation Notes

The ADR includes the requested conceptual `OrderSubmitted` sequence. Repository evidence shows the implemented event is currently named `OrderCreated`; the ADR names that distinction explicitly and makes no code or schema claim beyond it.

## Validation

Reviewed the ADR against the existing Order REST adapter, `OrderCreated` event, `HandleOrderCreatedService`, ADR-0002, ADR-0009, UC-01, and RF-UC-01. Confirmed documentation formatting with `git diff --check`.

## Tests

Not run. This was a documentation-only task, and tests were explicitly out of scope.

## Tradeoffs

Separating the public API boundary from orchestration avoids façade coupling but requires a future query/read model for UI visibility into workflow progress.

## Open Questions

* Which bounded context owns the eventual Order-and-saga status/timeline API?
* When, if ever, should `OrderCreated` be renamed or supplemented by `OrderSubmitted` at the contract level?
* What are the authorized, auditable operational recovery actions?

## Follow-ups

* Define a status/timeline read model in a scoped task.
* Create a new ADR before introducing an API Gateway, BFF, or administrative saga-control API.

## References

* [Task record](../tasks/ARB-ADR-0010-external-api-entry-point-and-orchestrator-responsibilities.md)
* [ADR-0010](../../adr/ADR-0010-external-api-entry-point-and-orchestrator-responsibilities.md)
* [ADR-0002](../../adr/ADR-0002-orchestrated-saga-with-kafka.md)
* [ADR-0009](../../adr/ADR-0009—GlobalInventoryAllocationOwnership.md)
* [RF-UC-01](../../rf/RF-UC-01-corporate-bulk-order.md)
