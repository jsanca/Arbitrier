# ARB-017 REVIEW — Pre-Saga Availability Negotiation

| Field | Value |
|-------|-------|
| **Reviewer** | Deep |
| **Date** | 2026-07-09 |
| **Task** | ARB-017 |
| **Status** | PLANNED |

---

## Verdict: **PASS WITH WARNINGS**

---

## Summary

ARB-017 cleanly separates advisory stock availability from authoritative saga reservation. The design replaces the original ("human inside the saga") approach with a pre-saga negotiation step that returns availability information and a recommended action — without creating an Order, starting a saga, or introducing any infrastructure dependencies. The implementation is architecturally consistent, well-tested, and ready for extension.

**One warning** (warehouseId propagation — see below). No blockers.

---

## Blockers

None.

---

## Warnings

### Warning 1 — `warehouseId` is lost between pre-check and saga reservation

The `warehouseId` is present at the pre-check stage (`PrepareCorporateBulkOrderCommand.warehouseId`) but is **not propagated** through the submission and saga command chain:

| Point | Has `warehouseId`? | Source |
|-------|--------------------|--------|
| `PrepareCorporateBulkOrderCommand` | ✅ | `order-service:.../inbound/PrepareCorporateBulkOrderCommand.java:19` |
| `InventoryAvailabilityPort.checkAvailability()` | ✅ passes it | `order-service:.../outbound/InventoryAvailabilityPort.java:24` |
| `SubmitCorporateBulkOrderCommand` | **❌ missing** | `order-service:.../inbound/SubmitCorporateBulkOrderCommand.java:12-15` |
| `OrderCreatedDomainEvent` | **❌ missing** | `order-service:.../event/OrderCreatedDomainEvent.java:19-23` |
| `HandleOrderCreatedCommand` | **❌ missing** | `orchestrator-service:.../inbound/HandleOrderCreatedCommand.java:11` |
| `ReserveStockSagaCommand` | **❌ missing** | `orchestrator-service:.../outbound/ReserveStockSagaCommand.java:9-12` |
| `ReserveStockCommand` (inventory inbound) | **✅ requires it** | `inventory-service:.../inbound/ReserveStockCommand.java:16` |

The authoritative reservation inside the saga **requires** `warehouseId` (`ReserveStockCommand` has `Require.notBlank`), but the saga has no way to obtain it — the `OrderCreatedDomainEvent` that triggers the saga does not carry a warehouse identifier.

**This is a real gap but does not block ARB-017 from DONE** because:
- No Kafka/adapters are wired yet — the gap only manifests when the saga is connected to inventory-service
- The `ConfigurableStockAvailabilityPort` ignores warehouseId, so current tests pass
- The implementation doc openly records this as OQ #5

**Required before production wiring**: Add `warehouseId` to `SubmitCorporateBulkOrderCommand` → `OrderCreatedDomainEvent` → `HandleOrderCreatedCommand` → `ReserveStockSagaCommand`. The `warehouseId` must flow from the buyer's pre-check through submission through the event to the saga command.

---

## Pre-Saga Design Review

### Design decision

The separation is clean and well-motivated:

| Concern | Location | Correct? |
|---------|----------|----------|
| Advisory stock check | `PrepareCorporateBulkOrderUseCase` (order-service) | ✅ |
| Buyer decision | Pre-saga, caller records before submission | ✅ |
| Authoritative reservation | Inside saga (`ReserveStockService`) | ✅ |
| Compensation on failure | Existing ARB-016 paths | ✅ unchanged |

- Human decision correctly occurs **before** saga start — no `AWAITING_CUSTOMER_DECISION` state involved ✅
- Pre-check is explicitly advisory; Javadoc on every result/interface repeats this ✅
- Saga reservation remains authoritative ✅
- Race conditions (stock changes between pre-check and reservation) acknowledged and handled by existing saga failure/compensation paths ✅
- `PrepareCorporateBulkOrderService` does not create an Order, start a saga, or publish events ✅

### Scope discipline

| Check | Status |
|-------|--------|
| No saga changes | ✅ (Saga domain model unchanged) |
| No Kafka | ✅ (no imports, no producers/consumers added) |
| No Avro | ✅ |
| No JPA | ✅ |
| No REST/gRPC production adapter | ✅ (`InventoryAvailabilityPort` is interface only; `StockAvailabilityPort` is interface only) |
| No persistence | ✅ (no repository.save, no event publish) |
| No timeout/scheduler | ✅ |
| No backorder order creation | ✅ (backorder is advisory only in the result) |

### Inventory query correctness (`CheckStockAvailabilityService`)

| Check | Result |
|-------|--------|
| Read-only query | ✅ — no `StockReservationRepository` dependency |
| No reservation created | ✅ — no domain state mutated |
| No repository save | ✅ |
| No domain event published | ✅ |
| `availableQuantity = min(stock, requested)` | ✅ (`CheckStockAvailabilityService.java:64`) |
| `backorderQuantity = max(0, requested - stock)` | ✅ (`CheckStockAvailabilityService.java:65`) |
| `fullyAvailable = stock >= requested` | ✅ (`CheckStockAvailabilityService.java:66`) |
| Full, partial, and zero-stock outcomes | ✅ (all three tested) |

### Order negotiation correctness (`PrepareCorporateBulkOrderService`)

| Check | Result |
|-------|--------|
| `PROCEED_FULL` only when every line fully available | ✅ (`PrepareCorporateBulkOrderService.java:119-120`: `allAvailable` check) |
| `ASK_CUSTOMER_ACCEPT_PARTIAL` when some stock exists | ✅ (line 122: `!availableLines.isEmpty()`) |
| `REJECT_NO_STOCK` when none available | ✅ (line 125: fallthrough) |
| `availableLines` = lines where `availableQuantity > 0` | ✅ (line 103-105) |
| `backorderLines` = lines where `backorderQuantity > 0` | ✅ (line 108-112) |
| Line may appear in both groups intentionally | ✅ documented in `PrepareCorporateBulkOrderResult` Javadoc |

### Customer decision model

`CustomerPreSagaDecision` has three values for v1:

| Decision | Intended use |
|----------|-------------|
| `ACCEPT_FULL` | Submit all originally requested quantities (valid when `PROCEED_FULL` recommended) |
| `ACCEPT_PARTIAL` | Submit only available quantities from the pre-check result |
| `CANCEL` | Abandon before any saga is started |

Sufficient for v1 ✅

`ACCEPT_PARTIAL` uses only available quantities (tested at `PrepareCorporateBulkOrderServiceTest.java:128-148`) ✅

No Order aggregate is created before final submission (`SubmitCorporateBulkOrderService` is the only path that calls `Order.create()`) ✅

**Open question**: Should `ACCEPT_FULL` be permitted after a partial recommendation? The current model leaves validation to the caller (UI/controller). If the buyer submits `ACCEPT_FULL` with full requested quantities after seeing `ASK_CUSTOMER_ACCEPT_PARTIAL`, the saga will attempt the full reservation. If stock is genuinely insufficient, the saga's existing failure/compensation paths handle it. This is architecturally correct — the saga reservation is authoritative — but the UX may be confusing. Defer to ARB-018.

---

## Warehouse Propagation

See Warning 1 above for the complete analysis.

**Cleanest flow** for `warehouseId`:

```
pre-check:     PrepareCorporateBulkOrderCommand.warehouseId   ✅ (exists)
submission:    SubmitCorporateBulkOrderCommand.warehouseId    ❌ (must add)
event:         OrderCreatedDomainEvent.warehouseId            ❌ (must add)
saga command:  HandleOrderCreatedCommand.warehouseId          ❌ (must add)
saga command:  ReserveStockSagaCommand.warehouseId            ❌ (must add)
inventory:     ReserveStockCommand.warehouseId                ✅ (already required)
```

The cleanest path avoids hidden state or UI-only memory — every actor along the chain carries the identifier explicitly.

**Recommendation**: Fix this before wiring any adaptation layer (gRPC/HTTP/Kafka) between order-service, orchestrator, and inventory-service. It is a data contract gap, not a design error — the infrastructure layer cannot make up the missing value.

---

## Decision Persistence

`CustomerPreSagaDecision` is currently an enum with no persistence.

| Risk | Severity | Mitigation |
|------|----------|------------|
| Browser/session loss | Buyer must re-run pre-check | Acceptable for v1 |
| Repeated submissions | Stale availability results → saga handles via failure/compensation | ✅ existing |
| Auditability | No record of what the buyer was shown or decided | Acceptable for v1 |

**Recommendation**: Persisting the pre-check result as a `PreparationId` or negotiation token is deferred to a future slice. The current ephemeral design is appropriate for the in-memory/in-development stage. Add a short-lived token when the first production adapter is wired.

---

## Cross-Service Boundaries

| Boundary | Verdict |
|----------|---------|
| `InventoryAvailabilityPort` in order-service application outbound ports | ✅ |
| `CheckStockAvailabilityUseCase` in inventory-service inbound ports | ✅ |
| No shared domain model between services | ✅ — order-service uses `AvailabilityLineQuery`/`AvailabilityLineResponse`, inventory-service uses `CheckStockAvailabilityCommand`/`CheckStockAvailabilityResult` |
| DTOs are transport-agnostic records | ✅ |

The port boundaries are clean. No service depends on the other's domain model. The `InventoryAvailabilityPort` returns `AvailabilityLineResponse` records — these are simple DTOs, not inventory-service domain types.

---

## Security

- `customerId`/`submittedByUserId` handling consistent with ARB-010 (`SubmitCorporateBulkOrderCommand` pattern) ✅
- No caller can prepare an order for an unauthorized customer once real adapters/controllers are wired — the `CustomerAccessPort` gate exists in `SubmitCorporateBulkOrderService` and would be called before the saga ✅
- No security concern leaked into domain (customerId/userId are strings, not security tokens) ✅

---

## Application-Service Style

| Check | Result |
|-------|--------|
| Services read as business stories | ✅ — `prepare()` method reads: build queries → fetch availability → compute results → determine action → return |
| No large/nested methods | ✅ — each step delegated to private method (`buildQueries`, `fetchAvailability`, `computeLineResults`, `determineAction`) |
| Single source of truth for line math | ✅ — `computeLineResults` is the only place where available/backorder/fullyAvailable is calculated |
| Semantic naming | ✅ — `PROCEED_FULL`, `ASK_CUSTOMER_ACCEPT_PARTIAL`, `REJECT_NO_STOCK`, `ACCEPT_PARTIAL` |

Bonus: `PrepareCorporateBulkOrderService` only takes `InventoryAvailabilityPort` — no `OrderRepository`, no `EventPublisher` — making it impossible to accidentally mutate state.

---

## Native Image Compatibility

| Check | Result |
|-------|--------|
| No `Class.forName()` | ✅ |
| No runtime classpath scanning | ✅ |
| No dynamic proxies | ✅ |
| No new `RuntimeHintsRegistrar` needed | ✅ |
| No reflection | ✅ |

All new code is plain records, enums, and interfaces.

---

## Tests

| Test class | Count (doc) | Count (source) | Match? |
|-----------|-------------|----------------|--------|
| `CheckStockAvailabilityServiceTest` | 9 | 9 | ✅ |
| `PrepareCorporateBulkOrderServiceTest` | 14 | 14 | ✅ |

### Coverage analysis

**CheckStockAvailabilityServiceTest** (9 tests):
- Fully available ✅
- Available quantity capped at requested ✅
- Partial availability (correct available/backorder split) ✅
- Backorder arithmetic ✅
- No stock (0 available for all lines) ✅
- Zero available = full backorder ✅
- Mixed availability per-line ✅
- Blank warehouseId throws ✅
- Empty lines throws ✅

**PrepareCorporateBulkOrderServiceTest** (14 tests):
- PROCEED_FULL when all available ✅
- No backorder lines when PROCEED_FULL ✅
- ASK_CUSTOMER_ACCEPT_PARTIAL when partially available ✅
- Available lines contain capped quantities ✅
- Backorder lines show unfulfilled quantities ✅
- REJECT_NO_STOCK when no stock ✅
- ACCEPT_PARTIAL available lines give correct submission quantities ✅
- CustomerPreSagaDecision enum values exist ✅
- No OrderRepository required (no side effects) ✅
- Port response reflected in result ✅
- Blank customerId throws ✅
- Blank userId throws ✅
- Blank warehouseId throws ✅
- Empty lines throws ✅

**No mutation/event side effects verified** — both services have no repository or publisher dependencies ✅

**All tests run without external infrastructure** — no Spring context, no Kafka, no JPA ✅

---

## Recommendations

1. **Address warehouseId propagation (see Warning 1)** before wiring any production adapter between order-service, orchestrator, and inventory-service. The cleanest approach: carry `warehouseId` explicitly through `SubmitCorporateBulkOrderCommand` → domain events → saga commands.

2. **Consider adding `ACCEPT_FULL` guard** at the application layer in ARB-018. Currently any caller can submit `ACCEPT_FULL` with full requested quantities even after receiving `ASK_CUSTOMER_ACCEPT_PARTIAL` or `REJECT_NO_STOCK`. While the saga compensates on failure, the UX would benefit from an explicit domain check: "ACCEPT_FULL is only valid when the recommended action was PROCEED_FULL."

3. **Pre-check `allAvailable` computation** in `PrepareCorporateBulkOrderResult` could be derived from `lineResults` rather than stored as a separate boolean parameter. Currently the `allAvailable` boolean is both computed in the service (`PrepareCorporateBulkOrderService.java:64`) AND stored in the result record — it could be derived from `lineResults.stream().allMatch(PrepareCorporateBulkOrderLineResult::fullyAvailable)` at the consumer side. Minor duplication, not a correctness issue.

---

## Decision

ARB-017 may be marked **DONE**.

The pre-saga availability negotiation is cleanly separated from saga execution. The `warehouseId` gap is a known open question documented in the implementation doc and does not block the current task — the in-memory adapter ignores warehouseId, and no production wiring exists yet. Address before ARB-018 (or the task that wires the first production adapter).
