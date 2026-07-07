# TC-UC-01-006 — Buyer Cancels Partial Order

| Field | Value |
|-------|-------|
| Status | Draft |
| Type | Integration / E2E |
| Requirement | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |

## Intention

Verify that buyer cancellation from the partial waiting state releases reservations and cancels the order.

## Context

This test covers the `CANCEL_ORDER` branch of UC-01.03.

## Decision or Requirement

Given an order in `AWAITING_CUSTOMER_DECISION`, when the buyer selects `CANCEL_ORDER`, then all reserved inventory is released and the order becomes `CANCELLED` with reason `customer_cancelled`.

## Inputs

- Order in `AWAITING_CUSTOMER_DECISION`.
- Buyer decision `CANCEL_ORDER`.

## Outputs

- `ReleaseStock` request for all reserved inventory.
- Final status `CANCELLED`.
- Cancellation reason `customer_cancelled`.

## Preconditions

- Partial reservation exists.
- Credit has not been consumed.

## Postconditions

- No reserved inventory remains for the cancelled order.
- No credit is consumed.

## Failure Behavior

- Duplicate cancellation decisions must not duplicate release effects.
- Compensation failure handling is OPEN QUESTION.

## Observability Expectations

- Timeline includes buyer cancellation and release stock compensation.
- Logs include cancellation reason.

## Test Evidence Placeholder

- Automated evidence pending implementation.

## Open Questions

- OPEN QUESTION: Whether cancellation emits a dedicated `OrderCancelled` event.
- OPEN QUESTION: Exact API behavior when decision is submitted twice.
