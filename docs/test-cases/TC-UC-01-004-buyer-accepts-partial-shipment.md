# TC-UC-01-004 — Buyer Accepts Partial Shipment

| Field | Value |
|-------|-------|
| Status | Superseded by TC-UC-01-003 / ARB-017 |
| Type | Integration / E2E |
| Requirement | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |

## Intention

Verify that accepting partial shipment confirms only available lines.

## Context

This test covers the `ACCEPT_PARTIAL` branch of UC-01.03.

> Historical test target. The current design resolves `ACCEPT_PARTIAL` before order and saga creation; use TC-UC-01-003. No active saga enters `AWAITING_CUSTOMER_DECISION`.

## Decision or Requirement

Given an order in `AWAITING_CUSTOMER_DECISION`, when the buyer selects `ACCEPT_PARTIAL`, then backorder lines are cancelled or released, credit is consumed only for confirmed lines, and the order becomes `PARTIALLY_CONFIRMED`.

## Inputs

- Order in `AWAITING_CUSTOMER_DECISION`.
- Reserved available lines.
- Backorder lines.
- Buyer decision `ACCEPT_PARTIAL`.

## Outputs

- Credit request for confirmed lines only.
- Final status `PARTIALLY_CONFIRMED`.

## Preconditions

- Some inventory is reserved.
- Some order lines are backordered.
- Credit service is available.

## Postconditions

- Available lines remain reserved.
- Backorder lines are not part of confirmed amount.
- Credit is consumed only for confirmed lines.

## Failure Behavior

- Credit rejection after accepting partial follows TC-UC-01-007.
- Credit timeout after accepting partial follows TC-UC-01-009.

## Observability Expectations

- Timeline includes customer decision and partial confirmation.
- Logs include decision value and confirmed amount correlation.

## Test Evidence Placeholder

- Automated evidence pending implementation.

## Open Questions

- OPEN QUESTION: Whether backorder lines are explicitly released, cancelled, or both in persistence.
- OPEN QUESTION: Exact event emitted for partial confirmation.
- OPEN QUESTION: Exact buyer-facing wording for partial confirmation.
