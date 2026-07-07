# TC-UC-01-010 — ReleaseStock Is Idempotent

| Field | Value |
|-------|-------|
| Status | Draft |
| Type | Unit / Integration |
| Requirement | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |

## Intention

Verify that releasing stock more than once does not corrupt inventory.

## Context

`ReleaseStock` is the documented compensation for credit rejection, buyer cancellation, buyer deferral, and credit timeout after reservation.

## Decision or Requirement

Given stock reservation exists or was already released, when `ReleaseStock` is processed more than once, then inventory remains consistent, no negative reservation exists, and duplicate command is safely ignored or returns an already-released result.

## Inputs

- Reservation identifier or equivalent release target.
- Duplicate `ReleaseStock` command or request.

## Outputs

- At most one effective release.
- Inventory consistency preserved.

## Preconditions

- Reservation exists or release has already been applied.

## Postconditions

- Reservation is released.
- No negative stock or reservation count exists.

## Failure Behavior

- Unknown reservation behavior is OPEN QUESTION.
- Duplicate command handling must be logged as idempotent suppression or already-released result.

## Observability Expectations

- Logs and metrics identify duplicate release handling.
- Trace links release attempts to the same `sagaId` and `orderId`.

## Test Evidence Placeholder

- Automated evidence pending implementation.

## Open Questions

- OPEN QUESTION: Exact reservation identifier shape.
- OPEN QUESTION: Exact response for already released reservation.
- OPEN QUESTION: Whether `ReleaseStock` is a Kafka command or synchronous API call.
