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

Unit tests implemented in ARB-007:
- `SubmitCorporateBulkOrderServiceTest` — 10 tests covering happy path, repository/publisher calls, and command validation.
- Tests run without Docker, Kafka, Postgres, Keycloak, or Schema Registry.
- Integration test (REST + Spring context) pending Keycloak integration.

## Open Questions

- OPEN QUESTION: Exact request/response payload not yet stabilized (endpoint `POST /api/orders` is live but subject to API design review).
- OPEN QUESTION: Exact `OrderCreated` Avro schema (contract exists in ARB-006 but Kafka adapter not yet wired).
- OPEN QUESTION: Idempotency key source for order submission (see TC-UC-01-011).
- OPEN QUESTION: correlationId propagation from HTTP header to command.
