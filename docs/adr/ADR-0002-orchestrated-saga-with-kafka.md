# ADR-0002 — Orchestrated Saga with Kafka

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-07-07 |

## Context

Arbitrier treats the use case as the primary citizen. UC-01 requires explicit business transitions, compensations, technical retry handling, and observable state. ARB-017 later moved the buyer's partial-availability decision before saga start.

## Decision

Use an explicit saga orchestrator for UC-01. Kafka is the messaging backbone for saga events and, pending schema decisions, may also carry commands.

The orchestrator owns saga transitions such as waiting for inventory/credit, compensation, completion, cancellation, and failed compensation. Customer-facing order outcomes remain owned by Order. `AWAITING_CUSTOMER_DECISION` is not on the active saga path; see ARB-017 and ADR-0009.

## Consequences

- Business decisions stay out of service adapters.
- Compensations are modeled as explicit saga actions.
- Event delivery is at-least-once, so idempotency is mandatory.

## Open Questions

- OPEN QUESTION: Should commands also go through Kafka, or only events?
- OPEN QUESTION: Exact topic names, partitioning keys, and schema names.
