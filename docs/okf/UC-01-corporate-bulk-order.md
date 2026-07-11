# UC-01 — Corporate Bulk Order with B2B Credit

| Field | Value |
|-------|-------|
| Status | Active — domain/application core implemented; runtime adapters pending |
| Date | 2026-07-07 |
| Source | `docs/okf/seeds/UC-01-corporate-bulk-order.md`, `docs/okf/seeds/UC-01-use-case-and-test-matrix.md` |

## Intention

Model a corporate bulk order as an orchestrated saga where inventory reservation, B2B credit consumption, human decisions for partial backorders, compensations, and final order visibility are explicit.

## Context

The corporate buyer prepares one or more SKU lines through the B2B portal. A read-only global availability check occurs before submission. If availability is partial, the buyer accepts available quantities or cancels before an Order or Saga exists. After submission, the order begins in `PENDING` and the orchestrator coordinates authoritative inventory and credit reservations.

The only documented terminal states for UC-01 are:

- `CONFIRMED`
- `PARTIALLY_CONFIRMED`
- `CANCELLED`

`AWAITING_CUSTOMER_DECISION` remains a legacy enum value but is not part of the implemented UC-01 saga path. ARB-017 moved the human decision before saga start.

## Decision or Requirement

UC-01 must preserve these guarantees:

- No order remains in an ambiguous state.
- Reserved inventory is released when the current saga does not confirm the reserved stock.
- Credit is never consumed for an order that ends as `CANCELLED`.
- Credit is consumed exactly for the amount that is confirmed.
- Partial availability requires an explicit buyer decision before order submission.
- The orchestrator owns business transitions; adapters must not hide business decisions.

## Inputs

- Authenticated corporate buyer.
- Corporate account with active B2B credit line.
- Order lines containing SKU and quantity.
- Product catalog availability.
- Global inventory availability; warehouse allocation remains internal to Inventory.
- Inventory events: `StockReserved`, `StockPartiallyReserved`.
- Credit events: `CreditApproved`, `CreditRejected`.
- Pre-saga buyer decisions: `ACCEPT_FULL`, `ACCEPT_PARTIAL`, `CANCEL`.

## Outputs

- Order created with `PENDING` status.
- `OrderCreated` event.
- Inventory reservation request.
- Credit validation and consumption request.
- `OrderConfirmed` event when fully confirmed.
- `PARTIALLY_CONFIRMED` order when accepted partial quantities are authoritatively reserved and credit-approved.
- `CANCELLED` order with a technical or business reason after submission; cancellation before submission creates no order.
- Visible saga status and event timeline.

## Preconditions

- Buyer is authenticated with Keycloak.
- Buyer has an active B2B credit line.
- Product catalog and warehouse inventory are available and up to date.
- The order contains one or more SKU lines.

## Postconditions

- Happy path: order is `CONFIRMED`, all requested stock is reserved, and credit is consumed for the full confirmed amount.
- Accepted partial path: only buyer-accepted quantities are submitted; the authoritative saga may end `PARTIALLY_CONFIRMED` when those quantities are confirmed.
- Pre-saga cancellation creates neither an Order nor a Saga and holds no stock or credit.
- Credit rejected path: order is `CANCELLED` with reason `insufficient_credit`, reserved inventory is released, and exact credit limit details are not exposed to the buyer.
- Timeout path: order is `CANCELLED` with reason `system_timeout`, and compensation runs according to the current saga step.

## Failure Behavior

- Inventory timeout before reservation: use the implemented attempt-count policy to decide retry or exhaustion; runtime scheduling/backoff is pending ARB-024. On exhaustion, compensate and do not consume credit.
- Credit timeout after stock reservation: use the same decision boundary; on exhaustion, request `ReleaseStock` and cancel through compensation.
- Credit rejection after stock reservation: release reserved inventory and cancel with `insufficient_credit`.
- Duplicate `OrderCreated`: do not create a duplicate saga or duplicate stock reservation.
- Duplicate `ReleaseStock`: leave inventory consistent and do not create negative reservation.

## Observability Expectations

- Every saga transition logs `sagaId`, `orderId`, state, transition, and reason when available.
- Every important transition produces traces and metrics.
- The future runtime dashboard shows order status, saga state, and event timeline. Buyer availability decisions are shown before submission.
- Compensation attempts and idempotent duplicate handling must be visible in logs and traces.

## Test Evidence

- Unit/application tests exist in each service module; persistence integration tests cover JPA adapters.
- Integration tests: see `docs/test-cases/TC-UC-01-001-create-pending-order.md` through `TC-UC-01-011-duplicate-order-created-idempotent.md`.
- E2E tests: see `docs/test-cases/TC-UC-01-012-saga-timeline-visible.md`.
- Contract tests load and generate types from the 26 Avro schemas in `server/contracts`.

## Open Questions

- OPEN QUESTION: What runtime timeout durations and backoff belong in ARB-024?
- OPEN QUESTION: What exact topic names and partitioning keys complete the Kafka runtime adapter set?
- OPEN QUESTION: What exact estimated shipping date rule is shown after confirmation?
