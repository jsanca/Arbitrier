# TC-UC-01-007 — Credit Rejected Compensation

| Field | Value |
|-------|-------|
| Status | Draft |
| Type | Integration |
| Requirement | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |

## Intention

Verify that credit rejection releases inventory and cancels the order.

## Context

This test covers UC-01.04.

## Decision or Requirement

Given inventory was reserved and credit line is insufficient, when credit-service emits `CreditRejected`, then `ReleaseStock` is emitted or requested, inventory reservation is released, the order becomes `CANCELLED`, reason is `insufficient_credit`, and exact credit limit is not exposed to the buyer.

## Inputs

- Reserved inventory.
- `CreditRejected`.

## Outputs

- `ReleaseStock` request.
- Final status `CANCELLED`.
- Cancellation reason `insufficient_credit`.

## Preconditions

- Order has reserved stock.
- Credit service can reject consumption.

## Postconditions

- Reserved inventory is released.
- Credit is not consumed.
- Buyer sees rejection without exact credit limit details.

## Failure Behavior

- Release stock command must be idempotent.
- Compensation failure handling is OPEN QUESTION.

## Observability Expectations

- Timeline includes credit rejection, compensation, and cancellation.
- Logs avoid exposing sensitive credit limit details.

## Test Evidence Placeholder

- Automated evidence pending implementation.

## Open Questions

- OPEN QUESTION: Exact buyer-facing rejection message.
- OPEN QUESTION: Exact sensitive credit fields that must be suppressed.
- OPEN QUESTION: Compensation failure escalation path.
