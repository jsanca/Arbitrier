# TC-UC-01-005 — Buyer Waits for Backorder

| Field | Value |
|-------|-------|
| Status | Deferred — not in current UC-01 implementation |
| Type | Integration |
| Requirement | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |

## Intention

Verify that waiting for backorder releases current reservations and ends the current saga.

## Context

This test covers the `WAIT_BACKORDER` branch of UC-01.03.

> Historical discovery target. ARB-017 removed in-saga customer waiting. A future backorder-deferral requirement must define a new flow before this test can become active.

## Decision or Requirement

Given an order in `AWAITING_CUSTOMER_DECISION`, when the buyer selects `WAIT_BACKORDER`, then all reserved inventory is released, a derived waiting order is created or requested, and the current saga ends as `CANCELLED` with reason `customer_deferred`.

## Inputs

- Order in `AWAITING_CUSTOMER_DECISION`.
- Buyer decision `WAIT_BACKORDER`.

## Outputs

- `ReleaseStock` request for all reserved inventory.
- Current order status `CANCELLED`.
- Cancellation reason `customer_deferred`.
- Derived waiting order creation or request.

## Preconditions

- Partial reservation exists.
- Buyer can submit a decision.

## Postconditions

- Current saga is terminal.
- Current reserved inventory is released.
- Credit is not consumed for the cancelled current saga.

## Failure Behavior

- Derived waiting order failure behavior is OPEN QUESTION.
- Compensation failure behavior is OPEN QUESTION.

## Observability Expectations

- Timeline includes customer decision, release request, and cancellation reason.
- Logs and traces distinguish business deferral from technical timeout.

## Test Evidence Placeholder

- Automated evidence pending implementation.

## Open Questions

- OPEN QUESTION: Should waiting backorder create a new order immediately or emit `BackorderRequested`?
- OPEN QUESTION: Exact derived order reference fields.
- OPEN QUESTION: Retry policy for derived waiting order creation.
