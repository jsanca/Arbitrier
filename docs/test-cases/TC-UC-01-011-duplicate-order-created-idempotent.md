# TC-UC-01-011 — Duplicate OrderCreated Is Idempotent

| Field | Value |
|-------|-------|
| Status | Draft |
| Type | Integration |
| Requirement | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |

## Intention

Verify that duplicate `OrderCreated` delivery does not create duplicate saga effects.

## Context

Kafka delivery is at-least-once, so the orchestrator must safely handle duplicate events.

## Decision or Requirement

Given `OrderCreated` was already processed, when the same `OrderCreated` event is consumed again, then the saga is not duplicated and no duplicate stock reservation is created.

## Inputs

- Duplicate `OrderCreated` event for the same order and saga.

## Outputs

- One saga instance.
- One effective inventory reservation request.

## Preconditions

- Initial `OrderCreated` has already started the saga.

## Postconditions

- Saga state remains consistent.
- Duplicate processing is recorded.

## Failure Behavior

- If duplicate detection storage is unavailable, behavior is OPEN QUESTION.

## Observability Expectations

- Duplicate suppression is logged with `sagaId`, `orderId`, and event identifier.
- Metrics include duplicate suppressed count.

## Test Evidence Placeholder

- Automated evidence pending implementation.

## Open Questions

- OPEN QUESTION: Exact event identifier used for idempotency.
- OPEN QUESTION: Exact inbox table or idempotency record design.
- OPEN QUESTION: Replay behavior during disaster recovery.
