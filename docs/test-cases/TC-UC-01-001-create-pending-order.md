# TC-UC-01-001 — Create Pending Order

| Field | Value |
|-------|-------|
| Status | Draft |
| Type | Integration |
| Requirement | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |

## Intention

Verify that an authenticated corporate buyer can submit valid SKU lines and create a `PENDING` order.

## Context

This test covers UC-01.01 from the matrix.

## Decision or Requirement

Given an authenticated corporate buyer with valid SKU lines, when the buyer submits an order, then order-service creates a `PENDING` order, emits `OrderCreated`, and assigns `sagaId` and `orderId`.

## Inputs

- Authenticated corporate buyer.
- One or more SKU lines.

## Outputs

- Order status `PENDING`.
- `OrderCreated` event.
- Assigned `sagaId` and `orderId`.

## Preconditions

- Buyer is authenticated with Keycloak.
- Buyer has active B2B credit line.
- Product catalog and inventory are available.

## Postconditions

- A single order exists for the submission.
- The saga can be started from `OrderCreated`.

## Failure Behavior

- Invalid order payload behavior is OPEN QUESTION.
- Duplicate submission behavior is covered by TC-UC-01-011.

## Observability Expectations

- Log order creation with `orderId`, `sagaId`, and `traceId`.
- Trace the submission request through order persistence and event publication.

## Test Evidence Placeholder

- Automated evidence pending implementation.

## Open Questions

- OPEN QUESTION: Exact order submission endpoint and payload.
- OPEN QUESTION: Exact `OrderCreated` Avro schema.
- OPEN QUESTION: Exact idempotency key source for order submission.
