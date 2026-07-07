# TC-UC-01-003 — Partial Backorder Moves to Human Decision

| Field | Value |
|-------|-------|
| Status | Draft |
| Type | Integration / E2E |
| Requirement | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |

## Intention

Verify that partial stock reservation pauses the saga for a buyer decision.

## Context

This test covers UC-01.03 before the buyer chooses how to proceed.

## Decision or Requirement

Given an order with at least one line lacking sufficient stock, when inventory reservation is attempted, then `StockPartiallyReserved` is emitted, the order becomes `AWAITING_CUSTOMER_DECISION`, and the buyer sees available and backorder lines.

## Inputs

- `OrderCreated`.
- Multiple order lines.
- Inventory availability that satisfies only some lines.

## Outputs

- `StockPartiallyReserved`.
- Order status `AWAITING_CUSTOMER_DECISION`.
- Buyer-visible available and backorder line groups.

## Preconditions

- Order is `PENDING`.
- Inventory service can identify available and unavailable lines.

## Postconditions

- Saga is waiting for `ACCEPT_PARTIAL`, `WAIT_BACKORDER`, or `CANCEL_ORDER`.
- No credit is consumed before the buyer decision.

## Failure Behavior

- If the buyer decision submission fails, retry behavior is OPEN QUESTION.
- If the waiting state expires, timeout policy is OPEN QUESTION.

## Observability Expectations

- Timeline marks entry into `AWAITING_CUSTOMER_DECISION`.
- Logs include available and backorder line identifiers without inventing hidden business rules.

## Test Evidence Placeholder

- Automated evidence pending implementation.

## Open Questions

- OPEN QUESTION: Does `AWAITING_CUSTOMER_DECISION` have a business expiration?
- OPEN QUESTION: Exact UI route and API for buyer decision.
- OPEN QUESTION: Exact line-level payload for partial reservation.
