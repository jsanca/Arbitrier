# TC-UC-01-002 — Happy Path Full Confirmation

| Field | Value |
|-------|-------|
| Status | Draft |
| Type | Integration / E2E |
| Requirement | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |

## Intention

Verify that full stock and approved credit produce a `CONFIRMED` order.

## Context

This test covers UC-01.02 and the main flow.

## Decision or Requirement

Given `OrderCreated`, enough inventory, and enough credit, when the saga runs, then `StockReserved`, `CreditApproved`, and `OrderConfirmed` are emitted and the final order status is `CONFIRMED`.

## Inputs

- Existing `OrderCreated`.
- Inventory with full available stock.
- Credit line with enough available balance.

## Outputs

- `StockReserved`.
- `CreditApproved`.
- `OrderConfirmed`.
- Final order status `CONFIRMED`.

## Preconditions

- Order is `PENDING`.
- Inventory and credit services are available.

## Postconditions

- Stock is reserved for all lines.
- Credit is consumed for the full confirmed amount.
- Buyer can see confirmed order.

## Failure Behavior

- If inventory times out, TC-UC-01-008 applies.
- If credit times out after reservation, TC-UC-01-009 applies.
- If credit rejects, TC-UC-01-007 applies.

## Observability Expectations

- Timeline includes order creation, stock reservation, credit approval, and order confirmation.
- Logs and traces include `sagaId`, `orderId`, and `traceId`.

## Test Evidence Placeholder

- Automated evidence pending implementation.

## Open Questions

- OPEN QUESTION: Exact event ordering guarantees.
- OPEN QUESTION: Exact shipping date rule shown to the buyer.
- OPEN QUESTION: Exact credit amount calculation details.
