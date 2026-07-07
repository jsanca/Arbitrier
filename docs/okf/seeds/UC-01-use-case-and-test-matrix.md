# UC-01 Use Case and Test Matrix

## Source of Truth

Primary source:
- docs/okf/seeds/UC-01-corporate-bulk-order.md

Do not invent business behavior.
If something is missing, mark it as OPEN QUESTION.

---

# Use Cases

## UC-01.01 Submit Corporate Bulk Order

Actor:
- Corporate Buyer

Goal:
- Submit a B2B bulk order with one or more SKU lines.

Preconditions:
- Buyer is authenticated with Keycloak.
- Buyer has active B2B credit line.
- Product catalog and inventory are available.

Expected result:
- Order is created as PENDING.
- OrderCreated event is emitted.

---

## UC-01.02 Confirm Order with Full Stock and Approved Credit

Trigger:
- OrderCreated event.

Flow:
- Reserve full stock.
- Consume corporate credit.
- Confirm order.

Expected result:
- Order status is CONFIRMED.
- Stock is reserved.
- Credit is consumed for confirmed amount.
- Buyer can see confirmed order.

---

## UC-01.03 Handle Partial Backorder with Customer Decision

Trigger:
- Inventory emits StockPartiallyReserved.

Flow:
- Orchestrator moves order to AWAITING_CUSTOMER_DECISION.
- Buyer sees available lines and backorder lines.
- Buyer chooses one of:
    - accept partial shipment
    - wait for backorder
    - cancel full order

Expected result:
- Accept partial → PARTIALLY_CONFIRMED.
- Wait backorder → CANCELLED with reason customer_deferred and derived waiting order created.
- Cancel → CANCELLED with reason customer_cancelled.

---

## UC-01.04 Cancel Order When Credit Is Rejected

Trigger:
- Credit service emits CreditRejected.

Expected result:
- Reserved inventory is released.
- Order status is CANCELLED.
- Cancellation reason is insufficient_credit.
- Credit limit details are not exposed to buyer.

---

## UC-01.05 Cancel Order on Downstream Timeout

Trigger:
- Inventory or credit service timeout after configured retries.

Expected result:
- Saga compensates according to current step.
- Order status is CANCELLED.
- Cancellation reason is system_timeout.

---

## UC-01.06 View Saga Status

Actor:
- Buyer or internal operator.

Goal:
- See current order/saga status.

Expected result:
- UI displays order status, saga state, event timeline, and pending human decision if any.

---

## UC-01.07 Execute Customer Decision

Actor:
- Corporate Buyer.

Goal:
- Continue or cancel a saga waiting for human decision.

Allowed decisions:
- ACCEPT_PARTIAL
- WAIT_BACKORDER
- CANCEL_ORDER

Expected result:
- Decision is persisted.
- Saga resumes.
- Final state follows UC-01.03 rules.

---

# Test Cases

## TC-UC-01-001 Create Pending Order

Given:
- Authenticated corporate buyer.
- Valid SKU lines.

When:
- Buyer submits order.

Then:
- order-service creates order with PENDING status.
- OrderCreated event is emitted.
- SagaId and OrderId are assigned.

Type:
- Integration.

---

## TC-UC-01-002 Happy Path Full Confirmation

Given:
- OrderCreated exists.
- Inventory has enough stock.
- Credit line has enough available balance.

When:
- Saga runs.

Then:
- StockReserved is emitted.
- CreditApproved is emitted.
- OrderConfirmed is emitted.
- Final order status is CONFIRMED.

Type:
- Integration / E2E.

---

## TC-UC-01-003 Partial Backorder Moves to Human Decision

Given:
- Order contains multiple lines.
- At least one line has insufficient stock.

When:
- Inventory reservation is attempted.

Then:
- StockPartiallyReserved is emitted.
- Order status becomes AWAITING_CUSTOMER_DECISION.
- Buyer is shown available vs backorder lines.

Type:
- Integration / E2E.

---

## TC-UC-01-004 Buyer Accepts Partial Shipment

Given:
- Order is AWAITING_CUSTOMER_DECISION.
- Some lines are reserved and some are backordered.

When:
- Buyer selects ACCEPT_PARTIAL.

Then:
- Backorder lines are cancelled/released.
- Credit is consumed only for confirmed lines.
- Order status becomes PARTIALLY_CONFIRMED.

Type:
- Integration / E2E.

---

## TC-UC-01-005 Buyer Waits for Backorder

Given:
- Order is AWAITING_CUSTOMER_DECISION.

When:
- Buyer selects WAIT_BACKORDER.

Then:
- All reserved inventory is released.
- A derived waiting order is created or requested.
- Current saga ends as CANCELLED.
- Cancellation reason is customer_deferred.

Type:
- Integration.

---

## TC-UC-01-006 Buyer Cancels Partial Order

Given:
- Order is AWAITING_CUSTOMER_DECISION.

When:
- Buyer selects CANCEL_ORDER.

Then:
- All reserved inventory is released.
- Order status becomes CANCELLED.
- Cancellation reason is customer_cancelled.

Type:
- Integration / E2E.

---

## TC-UC-01-007 Credit Rejected Compensation

Given:
- Inventory was reserved.
- Credit line is insufficient.

When:
- Credit service emits CreditRejected.

Then:
- ReleaseStock command is emitted.
- Inventory reservation is released.
- Order status becomes CANCELLED.
- Cancellation reason is insufficient_credit.
- Exact credit limit is not exposed to buyer.

Type:
- Integration.

---

## TC-UC-01-008 Inventory Timeout

Given:
- OrderCreated exists.
- Inventory service does not respond.

When:
- Retry policy is exhausted.

Then:
- Order status becomes CANCELLED.
- Cancellation reason is system_timeout.
- No credit is consumed.

Type:
- Integration.

---

## TC-UC-01-009 Credit Timeout After Stock Reserved

Given:
- Stock has been reserved.
- Credit service does not respond.

When:
- Retry policy is exhausted.

Then:
- ReleaseStock command is emitted.
- Order status becomes CANCELLED.
- Cancellation reason is system_timeout.
- Credit is not consumed.

Type:
- Integration.

---

## TC-UC-01-010 ReleaseStock Is Idempotent

Given:
- Stock reservation exists or was already released.

When:
- ReleaseStock command is processed more than once.

Then:
- Inventory remains consistent.
- No negative reservation exists.
- Duplicate command is safely ignored or returns already-released result.

Type:
- Unit / Integration.

---

## TC-UC-01-011 Duplicate OrderCreated Is Idempotent

Given:
- OrderCreated event was already processed.

When:
- Same OrderCreated event is consumed again.

Then:
- Saga is not duplicated.
- No duplicate stock reservation is created.

Type:
- Integration.

---

## TC-UC-01-012 Saga Timeline Is Visible

Given:
- Order has emitted multiple saga events.

When:
- Buyer or operator opens dashboard.

Then:
- UI displays current status.
- UI displays event timeline.
- UI displays pending action if saga is waiting for customer decision.

Type:
- E2E Playwright.

---

# Open Questions

- Should payment-service exist, or should this project use credit-service only?
- Should commands also go through Kafka, or only events?
- Should customer decision be exposed through order-service or orchestrator-service?
- Should waiting backorder create a new order immediately or emit BackorderRequested?
- Should SSE or WebSocket be used for saga status updates?
- What is the timeout SLA for inventory and credit?
- What roles are needed in Keycloak for buyer/operator/admin?