# TC-UC-01-009 — Credit Timeout After Stock Reserved

| Field | Value |
|-------|-------|
| Status | Draft |
| Type | Integration |
| Requirement | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |

## Intention

Verify that credit timeout after inventory reservation triggers stock release.

## Context

This test covers UC-01.05 when credit-service does not respond after stock reservation.

## Decision or Requirement

Given stock has been reserved and credit-service does not respond, when retry policy is exhausted, then `ReleaseStock` is emitted or requested, order status becomes `CANCELLED`, reason is `system_timeout`, and credit is not consumed.

## Inputs

- Reserved stock.
- Credit service timeout.

## Outputs

- `ReleaseStock` request.
- Final status `CANCELLED`.
- Cancellation reason `system_timeout`.

## Preconditions

- Order has reserved stock.
- Credit has not been approved.

## Postconditions

- Reserved inventory is released.
- Credit is not consumed.

## Failure Behavior

- Duplicate release requests must be idempotent.
- Compensation failure handling is OPEN QUESTION.

## Observability Expectations

- Timeline includes stock reservation, credit timeout, release stock, and cancellation.
- Retry exhaustion is visible in logs, metrics, and traces.

## Test Evidence Placeholder

- Automated evidence pending implementation.

## Open Questions

- OPEN QUESTION: Exact credit timeout SLA.
- OPEN QUESTION: Exact retry count and backoff.
- OPEN QUESTION: Exact event emitted when cancellation follows a timeout.
