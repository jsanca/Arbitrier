# UC-01: Corporate Bulk Order with B2B Credit

**Status:** Draft
**Last updated:** 2026-07-06

---

## Primary Actor

Corporate buyer (B2B) — representative of an Office Depot client company with an enabled corporate credit line.

## Secondary Actors

- **Warehouse System** — inventory system by warehouse/region.
- **Credit System** — system (internal or external) for B2B credit line validation and consumption.
- **Orchestrator (Saga)** — coordinates the flow, not a business actor but participates in the technical flow.

## Stakeholders and Their Interests

- **Corporate buyer**: wants to complete their order quickly, with clear visibility if something is unavailable.
- **Office Depot (finance)**: wants to ensure no merchandise is shipped without credit backing.
- **Office Depot (operations/warehouse)**: wants to avoid overselling reserved inventory.

## Preconditions

- The buyer is authenticated (Keycloak) and their corporate account has an active B2B credit line.
- The product catalog and warehouse inventory are available and up to date.

## Minimum Guarantee (in case of failure)

- No order remains in an ambiguous state: every order ends in `CONFIRMED`, `PARTIALLY_CONFIRMED`, or `CANCELLED`.
- Reserved inventory is released if the order is not confirmed (compensation).
- Credit is never consumed if the order ends up cancelled.

## Success Guarantee (postcondition)

- The order is `CONFIRMED` or `PARTIALLY_CONFIRMED`.
- The corresponding inventory is reserved (not just deducted from the visually available stock, but committed).
- Corporate credit was consumed exactly for the amount of what was confirmed.
- The buyer has visibility of the final status and which items, if any, are on backorder.

## Trigger

The corporate buyer submits an order with one or more lines (SKU + quantity) from the B2B portal.

---

## Main Flow (happy path)

1. The buyer puts together an order with multiple items and confirms the shipment.
2. The system (`order-service`) creates the order in `PENDING` status and emits the `OrderCreated` event.
3. The saga orchestrator receives `OrderCreated` and requests inventory reservation from `inventory-service` for each line.
4. `inventory-service` reserves the full stock of all lines and emits `StockReserved` (total).
5. The orchestrator requests credit validation and consumption from `credit-service` for the total order amount.
6. `credit-service` validates that the amount is within the available limit, consumes it, and emits `CreditApproved`.
7. The orchestrator marks the order as `CONFIRMED` and emits `OrderConfirmed`.
8. The buyer sees the confirmed order with an estimated shipping date.

## Alternative Flows

### A1: Partial Backorder — requires buyer decision

1a. In step 4, `inventory-service` detects that one or more lines do not have sufficient stock (partial backorder), and emits `StockPartiallyReserved` indicating which lines are available and which are not.
2a. The orchestrator transitions the order to `AWAITING_CUSTOMER_DECISION` and notifies the buyer (via UI/portal) showing the available lines vs the backordered lines.
3a. The buyer chooses one of:
   - **(a) Accept partial shipment**: continue only with the available lines, cancelling the backordered lines.
   - **(b) Wait for backorder**: keep the complete order on hold until stock arrives (outside the transactional scope of this saga; handled as a derived order / retry).
   - **(c) Cancel the entire order**: do not proceed with anything.
4a. Depending on the choice:
   - (a) → the orchestrator releases the reservation of the backordered lines, requests credit only for the amount of the confirmed lines, and continues in the main flow from step 5 with the subset. The order ends as `PARTIALLY_CONFIRMED`.
   - (b) → the orchestrator releases all inventory reservation (avoids blocking stock indefinitely), creates a "waiting order" referencing the original, and the current saga ends with `CANCELLED` reason `customer_deferred`.
   - (c) → the orchestrator releases all inventory reservation. The order ends with `CANCELLED` reason `customer_cancelled`.

### A2: Insufficient Credit

1a. In step 6, `credit-service` rejects the credit consumption (exceeds available line) and emits `CreditRejected`.
2a. The orchestrator executes compensation: releases all inventory reservation made in step 4.
3a. The order transitions to `CANCELLED` with reason `insufficient_credit`.
4a. The buyer is notified with the rejection details (without exposing the exact credit limit, per business policy — to be validated).

### A3: Timeout / downstream service failure

1a. If `inventory-service` or `credit-service` does not respond within the defined SLA (see NFR for saga timeout), the orchestrator retries according to Resilience4j policy (retry with backoff).
2a. If retries are exhausted, it is treated as a rejection (go to A2 or release reservation depending on which step failed) and the order transitions to `CANCELLED` with reason `system_timeout`.

## Future Extensions (out of scope for UC-01 v1)

- Optimal warehouse selection when the same SKU exists in multiple warehouses.
- Automatic backorder retry when new stock arrives (derived order from step A1-b).
- Multi-level credit approval for amounts exceeding a certain threshold.

---

## Design Notes (to expand in OKF)

- The buyer's decision in A1 introduces a **human step within the saga**, which means the orchestrator must support a state of indefinite waiting (`AWAITING_CUSTOMER_DECISION`) without an aggressive timeout — this is different from a technical timeout (A3). See `docs/okf/saga-orchestration.md` (pending creation) for the rationale behind this design.
- The release of reserved inventory in A1(b) and A1(c) is the same compensation operation (`ReleaseStock`), reusable — it is worth modeling it as an idempotent command from the start.
