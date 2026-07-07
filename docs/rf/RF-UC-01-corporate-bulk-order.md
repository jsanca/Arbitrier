# RF-UC-01 — Corporate Bulk Order

| Field | Value |
|-------|-------|
| Status | Draft |
| Date | 2026-07-07 |
| Use Case | [UC-01](../okf/UC-01-corporate-bulk-order.md) |

## Intention

Define the functional requirements for submitting and completing a corporate bulk order through an orchestrated saga.

## Context

UC-01 is the first documented business flow for Arbitrier. The use case starts when a corporate buyer submits order lines and ends only in `CONFIRMED`, `PARTIALLY_CONFIRMED`, or `CANCELLED`. The intermediate human waiting state is `AWAITING_CUSTOMER_DECISION`.

## Decision or Requirement

### RF-UC-01-001 Create Pending Order

The system must create a new order in `PENDING` status and emit `OrderCreated` when an authenticated corporate buyer submits valid SKU lines.

### RF-UC-01-002 Reserve Inventory Before Credit Consumption

After `OrderCreated`, the orchestrator must request inventory reservation for each line before requesting credit validation and consumption.

### RF-UC-01-003 Confirm Fully Available Order

When all stock is reserved and credit is approved, the order must transition to `CONFIRMED`.

### RF-UC-01-004 Pause on Partial Backorder

When inventory emits `StockPartiallyReserved`, the order must transition to `AWAITING_CUSTOMER_DECISION` and show available lines and backorder lines to the buyer.

### RF-UC-01-005 Execute Buyer Decision

The system must support buyer decisions `ACCEPT_PARTIAL`, `WAIT_BACKORDER`, and `CANCEL_ORDER`.

### RF-UC-01-006 Accept Partial Shipment

When the buyer selects `ACCEPT_PARTIAL`, the saga must continue only with available lines, release or cancel backorder lines, consume credit only for confirmed lines, and end as `PARTIALLY_CONFIRMED`.

### RF-UC-01-007 Wait for Backorder

When the buyer selects `WAIT_BACKORDER`, all reserved inventory must be released, the current saga must end as `CANCELLED` with reason `customer_deferred`, and a derived waiting order must be created or requested.

### RF-UC-01-008 Cancel Partial Order

When the buyer selects `CANCEL_ORDER`, all reserved inventory must be released and the order must end as `CANCELLED` with reason `customer_cancelled`.

### RF-UC-01-009 Cancel on Credit Rejection

When credit is rejected after stock is reserved, the system must release reserved inventory and cancel the order with reason `insufficient_credit`.

### RF-UC-01-010 Cancel on Downstream Timeout

When inventory or credit retries are exhausted, the saga must compensate according to the current step and cancel with reason `system_timeout`.

### RF-UC-01-011 Preserve Idempotency

Duplicate events or commands must not duplicate sagas, stock reservations, credit consumption, or release operations.

### RF-UC-01-012 Expose Saga Timeline

The buyer or internal operator must be able to see order status, saga state, event timeline, and pending human decision when applicable.

## Inputs

- Authenticated corporate buyer.
- Order lines with SKU and quantity.
- `OrderCreated`, `StockReserved`, `StockPartiallyReserved`, `CreditApproved`, and `CreditRejected` events.
- Buyer decision value: `ACCEPT_PARTIAL`, `WAIT_BACKORDER`, or `CANCEL_ORDER`.

## Outputs

- `PENDING`, `AWAITING_CUSTOMER_DECISION`, `CONFIRMED`, `PARTIALLY_CONFIRMED`, or `CANCELLED` order status.
- `OrderCreated` and `OrderConfirmed` events where applicable.
- Inventory reservation and release requests.
- Credit validation and consumption requests.
- Visible dashboard state and event timeline.

## Preconditions

- Buyer is authenticated with Keycloak.
- Buyer has active B2B credit line.
- Product catalog and inventory are available.
- The submitted order has at least one line.

## Postconditions

- Successful full order ends as `CONFIRMED`.
- Successful accepted partial order ends as `PARTIALLY_CONFIRMED`.
- Rejected credit, timeout, deferred backorder, or buyer cancellation ends as `CANCELLED`.
- Cancelled orders do not retain reserved inventory.
- Cancelled orders do not consume credit.

## Failure Behavior

- Credit rejection must not expose exact credit limit details to the buyer.
- Inventory timeout before reservation must cancel without credit consumption.
- Credit timeout after reservation must release reserved stock before ending the saga.
- Duplicate `OrderCreated` must not create a duplicate saga.
- Duplicate `ReleaseStock` must be harmless.

## Observability Expectations

- Log every state transition and compensation with `sagaId`, `orderId`, and `traceId`.
- Emit metrics for started, confirmed, partially confirmed, cancelled, timed out, compensated, and duplicate-suppressed sagas.
- Trace the complete flow across order, orchestrator, inventory, credit, and frontend status requests.

## Test Evidence Placeholder

- Evidence pending implementation.
- Target tests are documented in `docs/test-cases/TC-UC-01-001-create-pending-order.md` through `docs/test-cases/TC-UC-01-012-saga-timeline-visible.md`.

## Open Questions

- OPEN QUESTION: Exact request and response payloads for order submission.
- OPEN QUESTION: Exact event and command topic names.
- OPEN QUESTION: Whether customer decision is handled by order-service or orchestrator-service.
- OPEN QUESTION: Whether waiting backorder creates an order synchronously or emits a request.
- OPEN QUESTION: Exact retry counts, backoff policy, and timeout SLA.
- OPEN QUESTION: Exact buyer notification channel.
