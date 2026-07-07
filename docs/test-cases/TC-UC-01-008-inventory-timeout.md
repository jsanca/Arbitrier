# TC-UC-01-008 — Inventory Timeout

| Field | Value |
|-------|-------|
| Status | Draft |
| Type | Integration |
| Requirement | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |

## Intention

Verify that inventory timeout cancels the order without consuming credit.

## Context

This test covers UC-01.05 when inventory does not respond.

## Decision or Requirement

Given `OrderCreated` and inventory-service does not respond, when retry policy is exhausted, then order status becomes `CANCELLED`, reason is `system_timeout`, and no credit is consumed.

## Inputs

- `OrderCreated`.
- Inventory service timeout.

## Outputs

- Final status `CANCELLED`.
- Cancellation reason `system_timeout`.

## Preconditions

- Order is `PENDING`.
- Inventory service call or message response can time out.

## Postconditions

- No credit consumption occurs.
- No inventory reservation remains for the order.

## Failure Behavior

- If timeout happens after partial reservation, compensation behavior is OPEN QUESTION unless `StockPartiallyReserved` was received.

## Observability Expectations

- Retry attempts and exhausted retry outcome are logged and traced.
- Metrics include timeout cancellation.

## Test Evidence Placeholder

- Automated evidence pending implementation.

## Open Questions

- OPEN QUESTION: Exact inventory timeout SLA.
- OPEN QUESTION: Exact retry count and backoff.
- OPEN QUESTION: Whether inventory timeout is detected by request timeout, missing event deadline, or both.
