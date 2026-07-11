# TC-UC-01-003 — Pre-Saga Partial Availability and Buyer Decision

| Field | Value |
|-------|-------|
| Status | Updated (ARB-017) |
| Type | Unit / Integration |
| Requirement | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) §RF-UC-01-000 |

## Intention

Verify that when inventory is partially available, the system correctly identifies available
and backorder quantities, recommends `ASK_CUSTOMER_ACCEPT_PARTIAL`, and that accepted partial
quantities are the only quantities submitted to the saga.

## Context

This test case was originally titled "Partial Backorder Moves to Human Decision" and described
a saga pausing at `AWAITING_CUSTOMER_DECISION`. The design changed in ARB-017: the saga does
not wait for a human decision. The buyer decides before the saga starts.

## Decision or Requirement

Given an intended order with at least one line lacking sufficient stock:
1. `PrepareCorporateBulkOrderUseCase` must return `recommendedAction = ASK_CUSTOMER_ACCEPT_PARTIAL`.
2. `availableLines` must contain only lines with `availableQuantity > 0`, at available quantities.
3. `backorderLines` must contain only lines with `backorderQuantity > 0`.
4. If the buyer chooses `ACCEPT_PARTIAL`, the caller must submit with `availableLines` quantities
   — not the originally requested quantities.
5. The saga starts only after the buyer decision; no saga is created during the pre-check.

## Inputs

- Intended order lines with multiple SKUs.
- Inventory availability that satisfies only some lines (partial or zero availability on at least one).

## Outputs

- `PrepareCorporateBulkOrderResult` with `recommendedAction = ASK_CUSTOMER_ACCEPT_PARTIAL`.
- `availableLines` containing per-SKU available quantities.
- `backorderLines` containing lines where stock is insufficient.
- No `Order` aggregate created.
- No saga started.

## Preconditions

- `InventoryAvailabilityPort` is configured to return partial availability for at least one SKU.
- No `OrderRepository` or saga infrastructure is required.

## Postconditions

- `PrepareCorporateBulkOrderResult.allAvailable()` is `false`.
- `recommendedAction` is `ASK_CUSTOMER_ACCEPT_PARTIAL`.
- A line with `requestedQuantity=5, available=3` appears in `availableLines` with `availableQuantity=3`
  and in `backorderLines` with `backorderQuantity=2`.
- No Order aggregate is created during the pre-check.

## Failure Behavior

- If all lines have zero stock, `recommendedAction = REJECT_NO_STOCK` and `availableLines` is empty.
- Stock levels observed during the pre-check are advisory. If actual reservation later fails,
  existing saga compensation paths handle the rollback (ARB-016).

## Observability Expectations

- Application log includes `customerId`, requested lines, and `action=ASK_CUSTOMER_ACCEPT_PARTIAL`. Warehouse allocation is internal to Inventory.

## Test Evidence

| Test class | Method | Status |
|-----------|--------|--------|
| `PrepareCorporateBulkOrderServiceTest` | `prepare_returns_ask_customer_when_partial_availability` | Pass |
| `PrepareCorporateBulkOrderServiceTest` | `partial_result_available_lines_contain_available_quantities_only` | Pass |
| `PrepareCorporateBulkOrderServiceTest` | `partial_result_backorder_lines_show_unfulfilled_quantities` | Pass |
| `PrepareCorporateBulkOrderServiceTest` | `accepted_partial_decision_available_lines_give_quantities_for_submission` | Pass |
| `PrepareCorporateBulkOrderServiceTest` | `prepare_returns_reject_when_no_stock_for_any_line` | Pass |

## Open Questions

- OPEN QUESTION: UI route and API for submitting the pre-saga decision.
- OPEN QUESTION: Decision persistence if the buyer's session expires between pre-check and submit.
