# ADR-0002 — Orchestrated Saga with Kafka

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-07-07 |

## Context

Arbitrier treats the use case as the primary citizen. UC-01 requires explicit business transitions, compensations, human waiting, and observable state.

## Decision

Use an explicit saga orchestrator for UC-01. Kafka is the messaging backbone for saga events and, pending schema decisions, may also carry commands.

The orchestrator owns business transitions such as `AWAITING_CUSTOMER_DECISION`, `CONFIRMED`, `PARTIALLY_CONFIRMED`, and `CANCELLED`.

## Consequences

- Business decisions stay out of service adapters.
- Compensations are modeled as explicit saga actions.
- Event delivery is at-least-once, so idempotency is mandatory.

## Open Questions

- OPEN QUESTION: Should commands also go through Kafka, or only events?
- OPEN QUESTION: Exact topic names, partitioning keys, and schema names.
