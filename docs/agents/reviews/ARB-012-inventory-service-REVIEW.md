# ARB-012 — Inventory Service Application Slice — Review

| Field    | Value       |
|----------|-------------|
| Task     | ARB-012     |
| Status   | Implemented |
| Date     | 2026-07-08  |
| Reviewer | Deep        |

## Verdict

**PASS**

---

## Summary

ARB-012 delivers a clean inventory-service application slice with correct reservation and release behavior. The `ReserveStockService` pipeline reads as a clear business flow (`allocate → createReservation → save → publishEvent → return result`), and `reservation.status()` is the single source of truth for the outcome. Release idempotency is correctly implemented, and the REJECTED release no-op is well-documented and tested. No infrastructure leakage, no orchestration logic, clean hexagonal boundaries.

---

## 1. Scope Discipline: PASS

| Concern | Status |
|---------|--------|
| No Kafka producer/consumer | No Kafka deps; no Kafka imports in main code |
| No Avro generated class usage | `arbitrier-contracts` on classpath but never imported |
| No JPA entity | No `@Entity`; no JPA deps |
| No Postgres repository | In-memory test adapter only |
| No Flyway/Liquibase | Not present |
| No REST controller | Only `package-info.java` stubs in `adapter/inbound/rest/` |
| No saga orchestration | Only reservation and release — no saga coordination |
| No credit/order confirmation logic | Pure inventory domain |
| No security integration | No Spring Security deps |

---

## 2. Layer Boundaries: PASS

| Constraint | Evidence |
|------------|----------|
| Domain remains pure Java | `StockReservation`, `StockReservationLine`, etc. import only `com.arbitrier.platform.validation.Require` |
| Application does not depend on REST/JPA/Kafka/Avro | Services import platform + inventory domain + SLF4J only |
| Domain events are pure Java | `StockReservedDomainEvent`, `StockPartiallyReservedDomainEvent`, `StockRejectedDomainEvent`, `StockReleasedDomainEvent` — all plain records |
| Outbound ports in `application/port/outbound/` | `StockAvailabilityPort`, `StockReservationRepository`, `StockReservationEventPublisher` |
| Test adapters do not leak into production | `InMemoryStockReservationRepository`, `ConfigurableStockAvailabilityPort`, `RecordingStockReservationEventPublisher` are in test sources |
| ArchitectureTest enforces boundaries | 5 rules: domain→adapter, application→adapter, domain→Spring/JPA, domain→Avro/Kafka, application→Avro/Kafka |

---

## 3. Reservation Behavior: PASS

| Scenario | Code | Test |
|----------|------|------|
| Full stock → **RESERVED** | `ReserveStockService.createReservation()` → `StockReservation.fullyReserved()` | `full_stock_available_returns_reserved` |
| Partial stock → **PARTIALLY_RESERVED** | `StockReservation.partiallyReserved()` when `anyReserved && !allFullyReserved` | `partial_stock_returns_partially_reserved` |
| No stock → **REJECTED** | `StockReservation.rejected()` when no line has reserved quantity | `no_stock_available_returns_rejected` |
| Per-line: `reserved = min(requested, available)` | `allocate()`: `Math.min(line.quantity(), available)` | Verified through all three outcomes |
| No warehouse optimisation | Documented: "no cross-warehouse optimisation in this slice" | — |

The pipeline is clearly structured: `allocate → createReservation → save → publishEvent → return result`. The outcome is determined by domain factory methods and `reservation.status()` is returned directly in `ReserveStockResult` — it is the single source of truth.

---

## 4. Release Behavior: PASS

| Scenario | Behavior | Tests |
|----------|----------|-------|
| **RESERVED** → release | Transitions to RELEASED. Save + publish `StockReleasedDomainEvent`. | 3 tests (result, persist, event) |
| **PARTIALLY_RESERVED** → release | Same as RESERVED. | 2 tests (persist, event) |
| **RELEASED** → release (idempotent) | No-op. Returns same result, no save, no event. Domain `release()` returns `this`. | 2 tests (same result, no duplicate event) |
| **REJECTED** → release | No-op. No save, no event. Rationale: rejected reservation held no stock; `StockReleased` would be misleading. | 3 tests (result, no event, status unchanged) |
| Unknown reservation → release | Throws `IllegalArgumentException`. | 1 test (not found) |

---

## 5. Events: PASS

| Event | Published when |
|-------|---------------|
| `StockReservedDomainEvent` | All lines fully reserved (RESERVED) |
| `StockPartiallyReservedDomainEvent` | Some but not all lines reserved (PARTIALLY_RESERVED) |
| `StockRejectedDomainEvent` | No lines reserved (REJECTED). Note: carries no `lines` field — intentional. |
| `StockReleasedDomainEvent` | RESERVED or PARTIALLY_RESERVED transitions to RELEASED. Not emitted for REJECTED or idempotent calls. |

All events are pure Java records — no Avro/Kafka usage.

---

## 6. Transactionality / Outbox: PASS

| Criterion | Evidence |
|-----------|----------|
| `@Transactional` not required yet | JPA not yet wired; `ReserveStockService` javadoc explicitly states "will become `@Transactional` when JPA persistence is introduced" |
| DB + Kafka consistency via Outbox | `ReserveStockService` javadoc: "DB + Kafka consistency will be handled by the Outbox pattern" |
| Documentation is clear | Both the implementation note (open question #2) and service javadoc document the deferred approach |

---

## 7. Logging: PASS

All log statements contain only opaque identifiers and statuses — no PII:

| Location | Logged |
|----------|--------|
| `ReserveStockService` | `reservationId`, `orderId`, `warehouseId`, `outcome` (status enum) |
| `ReleaseStockService` (release) | `reservationId`, `orderId` |
| `ReleaseStockService` (idempotent) | `reservationId`, `orderId` |
| `ReleaseStockService` (REJECTED no-op) | `reservationId`, `orderId` |

---

## 8. Native Image Compatibility: PASS

| Risk | Status |
|------|--------|
| No reflection | None present |
| No dynamic proxies | None present |
| No `Class.forName()` | Not used |
| No runtime scanning | Not used |
| No `RuntimeHints` needed | All config is standard `@Configuration`/`@Bean` — AOT-processable |

---

## 9. Tests: PASS

| Test class | Tests | Coverage |
|------------|-------|----------|
| `ReserveStockServiceTest` | 18 | Full (4), partial (3), rejected (3), validation (7), result ID (1) |
| `ReleaseStockServiceTest` | 11 | RESERVED release (3), PARTIAL release (2), idempotent (2), REJECTED no-op (3), not found (1), validation (1), save interaction (1) |
| `StockReservationTest` | 11 | Domain invariants, state transitions, idempotent release, line validation |
| `ArchitectureTest` | 5 | Layer boundaries + Avro/Kafka isolation |
| `InventoryServiceApplicationIT` | 1 | Context load |
| **Total** | **46** | All pass without Docker/Kafka/Postgres/Keycloak/Schema Registry |

---

## 10. Documentation: PASS

| Criterion | Evidence |
|-----------|----------|
| Implementation note accurate | All files listed match file system. Reservation logic correctly documented. Release REJECTED decision documented with rationale. Idempotency documented. |
| Open questions valid | 1) REJECTED release + saga compensation (cross-service concern). 2) `StockAvailabilityPort` JPA implementation. 3) Reservation-not-found typing. 4) Duplicate reserve idempotency. 5) Partial allocation strategy. All valid, none hiding blockers. |
| Test case docs | TC-UC-01-002, TC-UC-01-003, TC-UC-01-010 are at Draft level — acceptable as requirement docs; unit tests provide implementation-level coverage |

---

## Domain/Application Concerns: None

The `StockReservationLine` invariant `reservedQuantity <= requestedQuantity` is correctly enforced in the compact constructor. The three outcome factory methods (`fullyReserved`, `partiallyReserved`, `rejected`) each validate their preconditions — `fullyReserved` rejects if any line isn't full, `partiallyReserved` rejects if all are full or none have reserved quantity.

**Minor observation (not a warning):** `ReleaseStockService` throws `IllegalArgumentException` for unknown reservations. The implementation note correctly identifies this should become a typed `InventoryProblemCode` when a REST/Kafka adapter is added.

---

## Recommendations

1. **When adding REST/Kafka adapters**: Replace `IllegalArgumentException` in `ReleaseStockService` with a typed problem code (e.g., `RESERVATION_NOT_FOUND`) mapped to HTTP 404.
2. **At the Kafka consumer layer**: Add an idempotency check for duplicate reserve commands using `IdempotencyStore` (ARB-005) before `ReserveStockService.reserve()` is called.

---

## Decision

**ARB-012 may be marked [DONE].** No blockers, no warnings, no code changes needed. The inventory service application slice is cleanly implemented with correct domain behavior, thorough test coverage, and clear documentation of deferred concerns.
