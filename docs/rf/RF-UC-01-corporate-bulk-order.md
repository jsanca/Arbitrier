# RF-UC-01 — Corporate Bulk Order

| Field | Value |
|-------|-------|
| Status | Active — reconciled with ARB-017/017B/018/019 |
| Date | 2026-07-07 |
| Use Case | [UC-01](../okf/UC-01-corporate-bulk-order.md) |

## Intention

Define the functional requirements for submitting and completing a corporate bulk order through an orchestrated saga.

## Context

UC-01 is the first documented business flow for Arbitrier. Availability negotiation and the buyer's partial-quantity decision occur before submission. The saga starts only after the buyer submits the selected quantities and ends in a confirmed or cancelled business outcome.

## Decision or Requirement

### RF-UC-01-000 Pre-Saga Availability Negotiation (ARB-017)

Before an order is submitted and a saga is started, the system must allow a buyer to check
stock availability for intended order lines. The check must:
- Return per-line available and backorder quantities.
- Return a recommended action: `PROCEED_FULL`, `ASK_CUSTOMER_ACCEPT_PARTIAL`, or `REJECT_NO_STOCK`.
- Not reserve stock, not create an Order, and not start a saga.
- Be advisory and non-binding — stock levels may change before the actual reservation.

The buyer must make an explicit pre-saga decision (`ACCEPT_FULL`, `ACCEPT_PARTIAL`, or `CANCEL`)
before the order is submitted. If `ACCEPT_PARTIAL`, only available quantities are submitted.
The saga reservation outcome remains authoritative regardless of the pre-check result.

### RF-UC-01-001 Create Pending Order

The system must create a new order in `PENDING` status and emit `OrderCreated` when an authenticated corporate buyer submits valid SKU lines.

### RF-UC-01-002 Reserve Inventory Before Credit Consumption

After `OrderCreated`, the orchestrator must request inventory reservation for each line before requesting credit validation and consumption.

### RF-UC-01-003 Confirm Fully Available Order

When all stock is reserved and credit is approved, the order must transition to `CONFIRMED`.

### RF-UC-01-004 Resolve Partial Availability Before Submission

When the advisory availability check returns partial quantities, the portal must show available and backorder quantities before submission. Inventory owns warehouse selection and exposes no warehouse choice to the buyer.

### RF-UC-01-005 Execute Pre-Saga Buyer Decision

The system must support `ACCEPT_FULL`, `ACCEPT_PARTIAL`, and `CANCEL`. Cancellation creates no Order or Saga.

### RF-UC-01-006 Accept Partial Shipment

When the buyer selects `ACCEPT_PARTIAL`, only accepted available quantities are submitted. The availability check remains advisory; the authoritative reservation may still differ.

### RF-UC-01-007 Backorder Deferral

Creating a waiting/backorder order is not part of the current implemented slice and must not be represented as an active saga state. A future requirement must define it explicitly.

### RF-UC-01-008 Cancel Partial Order

When the buyer cancels during availability review, no order, saga, stock reservation, or credit reservation is created.

### RF-UC-01-009 Cancel on Credit Rejection

When credit is rejected after stock is reserved, the system must release reserved inventory and cancel the order with reason `insufficient_credit`.

### RF-UC-01-010 Cancel on Downstream Timeout

When the attempt-count retry policy returns `EXHAUST`, the saga must compensate according to the current step. ARB-024 owns runtime duration, scheduling, and backoff.

### RF-UC-01-011 Preserve Idempotency

Duplicate events or commands must not duplicate sagas, stock reservations, credit consumption, or release operations.

### RF-UC-01-012 Expose Saga Timeline

The buyer or internal operator must be able to see order status, saga state, event timeline, and pending human decision when applicable.

## Inputs

- Authenticated corporate buyer.
- Order lines with SKU and quantity.
- `OrderCreated`, `StockReserved`, `StockPartiallyReserved`, `CreditApproved`, and `CreditRejected` events.
- Pre-saga buyer decision value: `ACCEPT_FULL`, `ACCEPT_PARTIAL`, or `CANCEL`.

## Outputs

- `PENDING`, `CONFIRMED`, `PARTIALLY_CONFIRMED`, or `CANCELLED` active order outcomes.
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
- Rejected credit or exhausted runtime attempt handling ends through cancellation/compensation.
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

## Test Evidence

- Domain, application, controller, architecture, and persistence integration tests exist in the service modules.
- Target tests are documented in `docs/test-cases/TC-UC-01-001-create-pending-order.md` through `docs/test-cases/TC-UC-01-012-saga-timeline-visible.md`.

## Open Questions

- OPEN QUESTION: Exact event and command topic names.
- OPEN QUESTION: Whether a future waiting-backorder capability creates an order synchronously or emits a request.
- OPEN QUESTION: Exact runtime backoff and timeout SLA (ARB-024); attempt-count decisions are implemented.
- OPEN QUESTION: Exact buyer notification channel.
