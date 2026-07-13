# ARB-015-REVIEW — Deep Review Saga Happy Path

| Field    | Value                                   |
|----------|-----------------------------------------|
| Task     | ARB-015                                 |
| Reviewer | Deep                                    |
| Date     | 2026-07-09                              |

---

## Verdict

**PASS WITH WARNINGS**

ARB-015 correctly models the UC-01 happy-path orchestration. The architectural boundaries are preserved, the domain is pure Java, the application services follow the established pipeline, and the test coverage is thorough. Three warnings and several recommendations below should be addressed before ARB-016 begins or when production adapters are wired.

---

## Summary

The implementation adds three event handlers (`HandleOrderCreatedService`, `HandleStockReservedService`, `HandleCreditApprovedService`), three outbound command ports (`ReserveStockCommandPublisher`, `ReserveCreditCommandPublisher`, `ConfirmOrderCommandPublisher`), and semantic saga transitions (`inventoryReserved()`, `creditApproved()`). The saga aggregate persists reservation IDs for future compensation. 32 new tests (92 total in orchestrator-service) cover happy path, validation, not-found, and event isolation. No infrastructure is required.

---

## Blockers

**None specific to ARB-015.** The service-level issue below is pre-existing and affects the entire orchestrator-service, not just ARB-015 code.

---

## Warnings

### W-1: No production adapters for any port interface

The `OrchestratorServiceConfiguration` wires `@Bean` methods that inject `ReserveStockCommandPublisher`, `ReserveCreditCommandPublisher`, and `ConfirmOrderCommandPublisher` — but these interfaces have **zero production implementations**. The same is true for `SagaRepository` and `SagaEventPublisher` (pre-existing from ARB-012/013/014). The Spring application context **cannot load** — all four adapter packages contain only `package-info.java` stubs.

Only `OrchestratorServiceTestConfiguration` (test tree) provides `@Primary` beans via recording adapters.

**Impact:** `OrchestratorServiceApplication.main()` throws `UnsatisfiedDependencyException` on startup.

**Recommendation:** Before the orchestrator-service can be deployed, production adapters must exist. Either:
- Add no-op/default implementations gated by `@ConditionalOnMissingBean` (allows tests and dev-mode to work without Kafka/JPA), or
- Split the wiring into a conditional `@Configuration` class (similar to `KafkaPublisherConfiguration` in order-service) gated by a property like `arbitrier.orchestrator.adapters.enabled`, or
- Implement the JPA and Kafka adapters in the next slice.

This is the highest-priority item. It is **not** an ARB-015 bug — the design intent is clear — but it must be resolved before any runtime scenario beyond unit tests.

### W-2: No idempotency check on HandleOrderCreatedService

`HandleOrderCreatedService.handle()` calls `repository.save(saga)` unconditionally with `Saga.start(...)`. If a Kafka consumer retry delivers the same `OrderCreated` event twice, the first saga instance is silently overwritten (lost). The `InMemorySagaRepository` uses `HashMap.put` — overwrite is immediate.

Documented as OPEN QUESTION 1 in the implementation note.

**Recommendation:** Add a `findById` check before `save` in `HandleOrderCreatedService` — either skip (idempotent accept) or throw (detect duplicate). The consumer layer should also be idempotent, but the application service should guard against accidental overwrite even without Kafka. This is a cheap defensive measure that costs one repository call.

### W-3: No stockReservationId correlation check

The `HandleStockReservedCommand` carries a `stockReservationId` echoed back by the inventory-service. The service stores it on the saga via `saga.inventoryReserved(command.stockReservationId())` but **never verifies** it matches the `stockReservationId` originally sent in `ReserveStockSagaCommand`. A misrouted or malicious event could store a wrong reservation ID.

Documented as OPEN QUESTION 2.

**Recommendation:** At minimum, add a comment and a `TODO` in the service. In ARB-016, consider loading the saga and asserting that `saga.stockReservationId()` is null (not yet set) before calling `inventoryReserved()` — this catches double-processing and misrouting without needing to store the expected ID separately.

### W-4: `creditApproved()` does not advance the saga step

```java
public Saga creditApproved(String creditReservationId) {
    ...
    return new Saga(id, orderId, customerId, status, currentStep,  // step unchanged
            customerDecision, stockReservationId, creditReservationId);
}
```

The step remains `VALIDATE_CREDIT` after `creditApproved()` — the caller must chain `.complete()` to set `COMPLETE_ORDER`. This is intentional (javadoc says "Call `complete()` immediately after"), but it means the saga is in `VALIDATE_CREDIT` step when credit is known to be approved. During partial-failure recovery in ARB-016, the distinction between "waiting for credit response" and "credit approved, completing order" is lost in the step value.

**Severity:** Low. The `COMPLETED` status disambiguates. But for compensation scenarios where the step determines which compensating commands to fire, the step alone is insufficient to distinguish "credit was never approved" from "credit was approved but complete never ran."

**Recommendation:** Either add a comment that `step` alone is not authoritative when `status == STARTED` (status + step together determine state), or create a separate step value (e.g., `CREDIT_APPROVED`) that `creditApproved()` transitions to, and let `complete()` transition to `COMPLETE_ORDER` from there.

---

## Scope Discipline

| Concern              | Status |
|----------------------|--------|
| No Kafka consumer    | ✅     |
| No Kafka producer    | ✅     |
| No Avro mapper       | ✅     |
| No REST              | ✅     |
| No JPA               | ✅     |
| No Postgres          | ✅     |
| No compensation      | ✅     |
| No timeout           | ✅     |
| No retry             | ✅     |
| No customer decision | ✅     |
| No backorder         | ✅     |

The three handlers are pure application services that depend only on ports and domain. In-memory test adapters fill all port dependencies. No infrastructure is required to run the 92 tests.

---

## Layer Boundaries

| Check                                     | Status | Detail |
|-------------------------------------------|--------|--------|
| Domain is pure Java                       | ✅     | Zero Spring/JPA/Kafka imports. Uses only `Require` from platform. |
| Application depends only on ports/domain  | ✅     | Imports: `application/port/inbound/*` (own interface), `application/port/outbound/*`, `domain/*`. |
| Command publishers are outbound ports      | ✅     | `ReserveStockCommandPublisher`, `ReserveCreditCommandPublisher`, `ConfirmOrderCommandPublisher` are interfaces in `application/port/outbound/`. |
| Domain events are pure Java records       | ✅     | `SagaCompletedDomainEvent` is a record with `SagaId` + `String` fields. |
| Test adapters under test sources          | ✅     | `RecordingReserveStockCommandPublisher` et al. in `src/test/java/`. |
| Architecture tests remain valid           | ✅     | ArchUnit tests pass (verified against code — no violations in new code). |

---

## Happy Path Review

### `HandleOrderCreatedService`

```
HandleOrderCreatedCommand(sagaId, orderId, customerId)
  → Saga.start(id, orderId, customerId)          STARTED, ORDER_CREATED
  → UUID.randomUUID() → stockReservationId
  → repository.save(saga)
  → eventPublisher.publishStarted(SagaStartedDomainEvent)
  → reserveStockCommandPublisher.publishReserveStock(ReserveStockSagaCommand)
  → HandleOrderCreatedResult(sagaId, stockReservationId)
```

Flow is correct and ordering is sound. The saga is persisted before events/commands are published — this matters when Outbox pattern (ADR-0005) is introduced.

The `stockReservationId` is generated by the orchestrator, not the inventory-service. This is a valid design choice (orchestrator provides correlation ID; inventory echoes it back). Documented in OPEN QUESTION 2.

### `HandleStockReservedService`

```
HandleStockReservedCommand(sagaId, stockReservationId)
  → loadSaga(sagaId) or throw
  → saga.inventoryReserved(stockReservationId)   STARTED, VALIDATE_CREDIT
  → UUID.randomUUID() → creditReservationId
  → repository.save(advanced)
  → eventPublisher.publishAdvanced(SagaAdvancedDomainEvent)
  → reserveCreditCommandPublisher.publishReserveCredit(ReserveCreditSagaCommand)
  → HandleStockReservedResult(sagaId, creditReservationId)
```

Flow is correct. `loadSaga()` with descriptive error message is clean. The saga is loaded fresh from the repository — no stale in-memory references.

### `HandleCreditApprovedService`

```
HandleCreditApprovedCommand(sagaId, creditReservationId)
  → loadSaga(sagaId) or throw
  → saga.creditApproved(creditReservationId)     STARTED, VALIDATE_CREDIT
  → .complete()                                   COMPLETED, COMPLETE_ORDER
  → repository.save(completed)
  → eventPublisher.publishCompleted(SagaCompletedDomainEvent)
  → confirmOrderCommandPublisher.publishConfirmOrder(ConfirmOrderSagaCommand)
  → HandleCreditApprovedResult(sagaId)
```

Flow is correct. The fluent `.creditApproved(...).complete()` chain is clean and readable. Both events (SagaCompleted) and commands (ConfirmOrder) are published after persistence.

**Ordering verification:** The three handlers execute sequentially in the correct business order: order created → reserve stock → stock reserved → reserve credit → credit approved → confirm order → saga completed.

---

## Saga Modeling

### Semantic transitions

`inventoryReserved(stockReservationId)` and `creditApproved(creditReservationId)` are **correctly preferred** over a generic `advance(SagaStep)`. They encode business meaning and store the relevant reservation ID in a single call. The naming matches the Ubiquitous Language.

### Reservation ID persistence

Both `stockReservationId` and `creditReservationId` are stored on the saga aggregate:
- `stockReservationId` set by `inventoryReserved()` ✅
- `creditReservationId` set by `creditApproved()` ✅
- Both are readable via getters for future compensation ✅
- Both thread correctly through all other transitions (immutability) ✅

### Step progression

```
Saga.start()           → ORDER_CREATED
.inventoryReserved()   → VALIDATE_CREDIT
.creditApproved()      → VALIDATE_CREDIT (unchanged — see W-4)
.complete()            → COMPLETE_ORDER
```

This is correct per the saga state machine. Steps `RESERVE_INVENTORY`, `AWAIT_CUSTOMER_DECISION`, `COMPENSATE_INVENTORY`, `COMPENSATE_CREDIT` are reserved for non-happy-path flows.

### Additional saga memory

The current fields (`orderId`, `customerId`, `stockReservationId`, `creditReservationId`, `customerDecision`) are sufficient for the happy path. For future compensation (ARB-016), the reservation IDs are available. No additional fields are needed at this stage.

---

## Compensation Readiness

| Capability                        | Status | Notes |
|-----------------------------------|--------|-------|
| Saga.cancel() / complete()        | ✅     | Pre-existing |
| Saga.compensate()                 | ✅     | Pre-existing |
| Saga.compensateInventory()        | ✅     | Pre-existing |
| Saga.compensateCredit()           | ✅     | Pre-existing |
| Saga.failCompensation()           | ✅     | Pre-existing |
| COMPENSATING status               | ✅     | Pre-existing |
| SagaCompensatedDomainEvent        | ✅     | Pre-existing |
| CompensationAction enum           | ✅     | Pre-existing (RELEASE_INVENTORY_RESERVATION, RELEASE_CREDIT_RESERVATION, NONE) |
| stockReservationId for release    | ✅     | Stored by ARB-015 |
| creditReservationId for release   | ✅     | Stored by ARB-015 |
| HandleStockRejectedService        | ❌     | ARB-016 |
| HandleCreditRejectedService       | ❌     | ARB-016 |
| inventoryRejected() transition    | ❌     | ARB-016 |
| creditRejected() transition       | ❌     | ARB-016 |
| Compensating command publishers   | ❌     | ARB-016 (ReleaseStockCommandPublisher, ReleaseCreditCommandPublisher) |
| Customer decision wiring          | ❌     | ARB-016 (HandleStockPartiallyReservedService) |

The reservation IDs stored by ARB-015 are the critical data foundation for compensation. The ARB-015 boundary is correctly drawn — compensation logic belongs to ARB-016. No ARB-015 types need modification for compensation readiness.

---

## Architectural Consistency

| Principle | Status | Evidence |
|-----------|--------|----------|
| App services read as business stories | ✅ | `handle()` methods delegate to well-named private methods; main method fits one screen |
| Domain owns invariants | ✅ | Terminal/COMPENSATING guards, blank-ID checks, status preconditions |
| One source of truth | ✅ | Saga aggregate is the single source for status, step, and reservation IDs |
| Ports define contracts | ✅ | Inbound: `Handle*UseCase`; Outbound: `*CommandPublisher` |
| Value Objects strongly typed | ✅ | `SagaId`, `SagaStatus`, `SagaStep`, `CustomerDecision`, `CompensationAction` |
| Business semantics over generic | ✅ | `inventoryReserved()` not `advance(SagaStep.RESERVE_INVENTORY)` |
| Interface return types on `@Bean` | ✅ | Returns `HandleOrderCreatedUseCase` not `HandleOrderCreatedService` |
| package-info.java present | ✅ | Every package has layer + module annotation |
| No platform imports from domain | ✅ | Only `Require` (platform utility, not business type) |

**Consistent with ARB-012/013/014:** The ARB-015 code follows the same patterns as the pre-existing `StartSagaService`, `AdvanceSagaService`, and `CompensateSagaService`. No drift.

---

## Native Image Compatibility

| Concern                | Status |
|------------------------|--------|
| No `Class.forName`     | ✅     |
| No dynamic proxies     | ✅     |
| No runtime scanning    | ✅     |
| No `RuntimeHints`      | ✅     |
| No unregistered reflection | ✅ |

All types are referenced explicitly. The `@Bean` methods construct services via `new`. No generated code, no reflection. No action needed.

---

## Logging Review

```
OrderCreated handled sagaId={} orderId={} customerId={} step={} status={}
StockReserved handled sagaId={} orderId={} stockReservationId={} step={} status={}
CreditApproved handled sagaId={} orderId={} creditReservationId={} step={} status={}
```

- ✅ Only opaque identifiers (sagaId, orderId, customerId, stockReservationId, creditReservationId)
- ✅ `customerId` is a business identifier (e.g., "CUST-001"), not PII
- ✅ Business-useful: each log shows which event was handled, current step and status
- ✅ Event type is recoverable from the log message prefix

---

## Test Coverage Review

### `HandleOrderCreatedServiceTest` (11 tests)

| Test | Status |
|------|--------|
| Returns saga ID | ✅ |
| Returns non-blank stockReservationId | ✅ |
| Persists saga with STARTED + ORDER_CREATED | ✅ |
| Persists saga with correct orderId + customerId | ✅ |
| Publishes SagaStarted with correct fields | ✅ |
| Publishes ReserveStock command | ✅ |
| Only started event (no advance/completed/compensated) | ✅ |
| Unique stock reservation IDs across calls | ✅ |
| Blank sagaId throws | ✅ |
| Blank orderId throws | ✅ |
| Blank customerId throws | ✅ |

### `HandleStockReservedServiceTest` (11 tests)

| Test | Status |
|------|--------|
| Returns saga ID | ✅ |
| Returns non-blank creditReservationId | ✅ |
| Advances step to VALIDATE_CREDIT | ✅ |
| Persists stockReservationId on saga | ✅ |
| Publishes SagaAdvanced with correct step | ✅ |
| Publishes ReserveCredit command | ✅ |
| Only advanced event (no started/completed) | ✅ |
| Unique credit reservation IDs across calls | ✅ |
| Unknown sagaId throws descriptive error | ✅ |
| Blank sagaId throws | ✅ |
| Blank stockReservationId throws | ✅ |

### `HandleCreditApprovedServiceTest` (10 tests)

| Test | Status |
|------|--------|
| Returns saga ID | ✅ |
| Transitions to COMPLETED + COMPLETE_ORDER | ✅ |
| Persists creditReservationId on saga | ✅ |
| Preserves stockReservationId on saga | ✅ |
| Publishes SagaCompleted with correct fields | ✅ |
| Publishes ConfirmOrder command | ✅ |
| Only completed event (no started/advanced) | ✅ |
| Unknown sagaId throws descriptive error | ✅ |
| Blank sagaId throws | ✅ |
| Blank creditReservationId throws | ✅ |

### Domain tests (`SagaTest`, 27 tests including ARB-015 additions)

Full coverage of `inventoryReserved()`, `creditApproved()`, `complete()`, terminal guards, null/blank validation.

### Coverage gap

No test verifies that `HandleCreditApprovedService` rejects a saga where `stockReservationId` is null (i.e., `creditApproved()` called without prior `inventoryReserved()`). The domain allows it (no invariant enforces ordering between the two). A safety check could be added.

---

## Documentation Review

The implementation note at `docs/implementation/ARB-015-saga-happy-path.md` is accurate and complete.

- ✅ File tables with purpose descriptions
- ✅ Step-by-step service behaviour matches code
- ✅ Test coverage table matches actual test counts
- ✅ Open questions are valid and actionable
- ✅ ARB-016 boundary is clear (compensation, partial reservation, customer decision)

The four open questions are:
1. Idempotency on OrderCreated (see W-2)
2. stockReservationId correlation (see W-3)
3. Transactionality (deferred to JPA phase)
4. W3C Trace Context propagation (consumer-layer concern)

---

## Recommendations

### Before ARB-016 begins

1. **Add idempotency check** in `HandleOrderCreatedService` — guard against saga overwrite with a `findById` check before `save`. A single `if (repository.findById(sagaId).isPresent()) { return; }` or throw protects against consumer retries.

2. **Add null-stockReservationId guard** in `HandleCreditApprovedService` — verify that the loaded saga has `stockReservationId() != null` before calling `creditApproved()`. This catches ordering violations early with a clear error.

3. **Add `TODO`/comment** in `HandleStockReservedService` noting that `stockReservationId` correlation should be verified when Kafka is wired.

### Before production deployment

4. **Implement production adapters** for all five port interfaces (`SagaRepository`, `SagaEventPublisher`, `ReserveStockCommandPublisher`, `ReserveCreditCommandPublisher`, `ConfirmOrderCommandPublisher`) — or gate wiring behind a conditional property so the application can start without them.

### Nice-to-have

5. **Reconsider `creditApproved()` step advancement** — if ARB-016 compensation logic needs to distinguish "credit was never requested" from "credit approved but completing", a new step value (`CREDIT_APPROVED`) between `VALIDATE_CREDIT` and `COMPLETE_ORDER` would make the intention explicit.

---

## Decision

ARB-015 may be marked **DONE**.

The three warnings (missing production adapters, no idempotency check, no correlation check) should be addressed before ARB-016 begins or before Kafka consumers are wired — not before marking ARB-015 complete, as they are explicitly scoped to future slices and documented as open questions.
