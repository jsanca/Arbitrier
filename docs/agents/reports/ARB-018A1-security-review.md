# ARB-018A1 — Architecture Security Review

## Status

DONE

## Review classification

PASS WITH WARNINGS

The architecture is fundamentally sound. No finding blocks the Kafka/REST integration phase
from beginning. All warnings have clear mitigation paths at known roadmap stages.

---

## Review summary

Performed across all 12 areas: domain boundaries, authentication, authorization, saga security,
event architecture, persistence, REST surface, runtime and infrastructure, logging and
observability, supply chain, native image readiness, and documentation.

Full review report: `docs/implementation/report-ARB-018A1-clio.txt`

---

## ARB-018A1-FIX — Implemented findings

The following findings from the security review were addressed as minimal, in-scope domain
invariant and validation improvements.

### F1 — Saga.failCompensation() terminal-state guard (Finding S1)

**File:** `server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/domain/model/Saga.java`

**Finding:** `failCompensation()` could overwrite any saga status — including `COMPLETED`,
`CANCELLED`, and `FAILED_COMPENSATION` itself — because it had no guard. All other lifecycle
transition methods already protect against terminal states via `Require.isTrue(!status.isTerminal())`.

**Change:** Added the same terminal-state guard to `failCompensation()`.

**Invariant enforced:**
```
terminal saga (COMPLETED | CANCELLED | FAILED_COMPENSATION)
  ↓ failCompensation()
  → throws IllegalArgumentException
```

**Tests added to `SagaTest`:**
- `fail_compensation_on_completed_saga_throws`
- `fail_compensation_on_cancelled_saga_throws`
- `fail_compensation_on_failed_compensation_saga_throws`

---

### F2 — Credit domain event constructor validation (Finding D2)

**Files:**
- `server/credit-service/src/main/java/com/arbitrier/credit/domain/event/CreditApprovedDomainEvent.java`
- `server/credit-service/src/main/java/com/arbitrier/credit/domain/event/CreditRejectedDomainEvent.java`
- `server/credit-service/src/main/java/com/arbitrier/credit/domain/event/CreditReleasedDomainEvent.java`

**Finding:** All three Credit domain events had no compact constructor validation. Every other
domain event in the codebase (Inventory `StockReservedDomainEvent`, `StockRejectedDomainEvent`;
Orchestrator `CreditTimedOutDomainEvent`, etc.) uses `Require.notNull` / `Require.notBlank`
in its compact constructor. Credit events were the only exception.

**Changes:** Added compact constructors to all three events:

| Event | Validated fields |
|-------|-----------------|
| `CreditApprovedDomainEvent` | `reservationId` (notNull), `orderId` (notBlank), `customerId` (notBlank), `amount` (notNull) |
| `CreditRejectedDomainEvent` | `reservationId` (notNull), `orderId` (notBlank), `customerId` (notBlank), `amount` (notNull) |
| `CreditReleasedDomainEvent` | `reservationId` (notNull), `orderId` (notBlank) |

**Tests added:** New `CreditDomainEventTest.java` in `server/credit-service/src/test/java/com/arbitrier/credit/domain/` — 13 tests covering valid construction and all null/blank rejection cases for all three events.

---

### F3 — Saga transition guards: stockRejected() and creditRejected() (Finding S6, partial)

**File:** `server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/domain/model/Saga.java`

**Finding:** `stockRejected()` and `creditRejected()` checked `!isTerminal()` but did not prevent
execution from `COMPENSATING` status. This allowed:
- `stockRejected()` from `COMPENSATING` → changes saga to `CANCELLED`, bypassing the
  `inventoryReleased()` compensation path.
- `creditRejected()` from `COMPENSATING` → re-enters `COMPENSATING` with `COMPENSATE_INVENTORY`
  step, resetting the step in the middle of an active compensation.

Both scenarios represent invalid lifecycle transitions with no valid business meaning.

**Changes:**
- `stockRejected()`: added `Require.isTrue(status != COMPENSATING, ...)` guard
- `creditRejected()`: added `Require.isTrue(status != COMPENSATING, ...)` guard (alongside the
  existing `!isTerminal()` and `stockReservationId != null` checks)

**Tests added to `SagaTest`:**
- `stock_rejected_on_compensating_saga_throws`
- `credit_rejected_on_compensating_saga_throws`

---

## Formally deferred finding

### D-RETRY — Retry counter ownership (Finding S4)

**Finding:** The `HandleCreditTimeoutService` and `HandleInventoryTimeoutService` rely on
`attemptNumber` supplied by the caller via `HandleCreditTimeoutCommand` /
`HandleInventoryTimeoutCommand`. The Saga aggregate does not persist the attempt count. A caller
could supply `attemptNumber=1` indefinitely, keeping the saga in a retry loop and bypassing the
`maxAttempts` limit.

**Why deferred:**

1. **Schema migration required.** Adding `attemptNumber` to `SagaEntity` requires a Flyway
   `V2__add_retry_counter.sql` migration. This migration affects all four services' test
   infrastructure and should be introduced in the dedicated retry implementation task, not here.

2. **Scheduler not yet implemented.** The timeout trigger (the component that will generate
   `HandleInventoryTimeoutCommand` / `HandleCreditTimeoutCommand` with a monotonically increasing
   `attemptNumber`) does not yet exist. Server-side counter enforcement only makes sense once the
   caller is the scheduler, not a test harness or stub.

3. **Scope constraint.** This task was explicitly scoped to minimal domain invariant and
   validation improvements. Retry ownership redesign changes the outbound command contract,
   inbound port, application services, JPA entity, and Flyway migration — disproportionate
   to the scope of this fix task.

**Deferral target:** ARB-018 extension or the dedicated saga timeout/retry infrastructure slice.
When the scheduler is implemented, `attemptNumber` should be owned by the Saga and incremented
on each retry, with the caller's provided value validated against the stored count or rejected
entirely.

---

## Roadmap-owned findings

The following findings from the full security review are explicitly assigned to future slices and
were not implemented in this task.

| Finding | Severity | Roadmap owner |
|---------|----------|---------------|
| A1 — No SecurityConfiguration in credit/inventory/orchestrator | HIGH | REST/Kafka integration slice |
| A2 — No JwtAuthenticationConverter | MEDIUM | Authorization slice |
| A3 — No audience validation | MEDIUM | SecurityConfiguration slice |
| Z1 — AllowAllCustomerAccessAdapter (tenant isolation placeholder) | HIGH | Authorization slice |
| Z2 — No method-level security (@PreAuthorize) | MEDIUM | Authorization slice |
| S2 — CompensateSagaUseCase/AdvanceSagaUseCase unauthenticated | HIGH | Kafka consumer + authorization slice |
| S3 — No duplicate event detection beyond OrderCreated | MEDIUM | IdempotencyStore (ARB-005) |
| S5 — No timeout scheduler — sagas stall indefinitely | MEDIUM | Timeout infrastructure (ARB-018 extension) |
| S6 (remaining) — stockRejected() from non-WAITING_FOR_INVENTORY | LOW | Tighter guards when Kafka consumers are wired |
| S7 — Credit compensation path not wired | LOW | Saga compensation extension (ARB-017) |
| E1 — Kafka PLAINTEXT | HIGH | Production deployment (not an architecture concern) |
| E2 — No event source verification | MEDIUM | Kafka consumer slice + Avro contracts |
| E3 — No replay protection | MEDIUM | IdempotencyStore (ARB-005) |
| R2 — No request size limits | MEDIUM | REST API security ADR |
| R3 — No CORS configuration | MEDIUM | REST integration slice |
| R4 — Actuator endpoints public | MEDIUM | REST API security ADR |
| R6 — No REST idempotency key | LOW | IdempotencyStore (ARB-005) |
| L1 — SafeLoggable has zero implementations | MEDIUM | Observability/logging slice |
| C1 — No vulnerability scanning | MEDIUM | CI hardening |
| N1 — No RuntimeHintsRegistrar | MEDIUM | ARB-004B (native image) |
| D5 — No API security ADR | MEDIUM | Documentation — before REST APIs added to all services |
| D6 — No threat model | MEDIUM | Documentation |

---

## Files changed

### Production

| File | Change |
|------|--------|
| `server/orchestrator-service/.../domain/model/Saga.java` | `failCompensation()` — added `!isTerminal()` guard; `stockRejected()` — added `!COMPENSATING` guard; `creditRejected()` — added `!COMPENSATING` guard |
| `server/credit-service/.../domain/event/CreditApprovedDomainEvent.java` | Added compact constructor with `Require` validation |
| `server/credit-service/.../domain/event/CreditRejectedDomainEvent.java` | Added compact constructor with `Require` validation |
| `server/credit-service/.../domain/event/CreditReleasedDomainEvent.java` | Added compact constructor with `Require` validation |

### Tests

| File | Change |
|------|--------|
| `server/orchestrator-service/.../domain/SagaTest.java` | +5 tests: 3 `failCompensation` terminal-state guards + 1 `stockRejected` COMPENSATING guard + 1 `creditRejected` COMPENSATING guard |
| `server/credit-service/.../domain/CreditDomainEventTest.java` | New — 13 tests for all three Credit domain event constructors |

---

## Test results

```
BUILD SUCCESS
contracts:             20 tests, 0 failures, 0 errors
platform:              83 tests, 0 failures, 0 errors
credit-service:        79 tests, 0 failures, 0 errors  (+13 new)
orchestrator-service: 234 tests, 0 failures, 0 errors  (+5 new)
```
