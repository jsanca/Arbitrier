# ARB-013 — Credit Service Application Slice

| Field  | Value                |
|--------|----------------------|
| Task   | ARB-013              |
| Status | Done                 |
| Date   | 2026-07-08           |
| Fixup  | ARB-013-FIX-001      |

## Summary

First application slice for credit-service: `ReserveCreditUseCase` and `ReleaseCreditUseCase`
with full domain event coverage and idempotent release. No JPA, Kafka, Avro, REST, or Docker
required to run tests.

ARB-013-FIX-001 applied the following post-review cleanups:
- `Money.canCover(Money)` added to domain for currency-safe comparison (currency mismatch fails fast).
- `ReserveCreditService` uses `available.canCover(command.amount())` instead of raw `BigDecimal` comparison.
- `CreditServiceConfiguration` `@Bean` methods now return port interfaces (`ReserveCreditUseCase`, `ReleaseCreditUseCase`) instead of implementation classes.
- `InMemoryStockReservationRepository` (inventory-service test adapter) updated to use `StockReservationId` as the map key instead of `String`, matching credit-service adapter style.

---

## Files Created

### Domain events

| File | Purpose |
|------|---------|
| `domain/event/CreditApprovedDomainEvent.java` | Credit reservation approved (sufficient available balance). |
| `domain/event/CreditRejectedDomainEvent.java` | Credit reservation rejected (insufficient available balance). |
| `domain/event/CreditReleasedDomainEvent.java` | Approved credit reservation released (APPROVED → RELEASED only). |

### Application — inbound ports

| File | Purpose |
|------|---------|
| `application/port/inbound/ReserveCreditUseCase.java` | Input port for credit reservation. |
| `application/port/inbound/ReleaseCreditUseCase.java` | Input port for credit release (idempotent). |
| `application/port/inbound/ReserveCreditCommand.java` | Command: orderId, creditReservationId, customerId, amount. |
| `application/port/inbound/ReleaseCreditCommand.java` | Command: creditReservationId. |
| `application/port/inbound/ReserveCreditResult.java` | Result: reservationId + outcome (APPROVED / REJECTED). |
| `application/port/inbound/ReleaseCreditResult.java` | Result: reservationId. |

### Application — outbound ports

| File | Purpose |
|------|---------|
| `application/port/outbound/CreditLimitPort.java` | Queries available credit for a customer. |
| `application/port/outbound/CreditReservationRepository.java` | Persists and loads `CreditReservation` aggregates. |
| `application/port/outbound/CreditReservationEventPublisher.java` | Publishes all three domain events. |

### Application services

| File | Purpose |
|------|---------|
| `application/service/ReserveCreditService.java` | Implements `ReserveCreditUseCase`. |
| `application/service/ReleaseCreditService.java` | Implements `ReleaseCreditUseCase`. |

### Configuration

| File | Purpose |
|------|---------|
| `config/CreditServiceConfiguration.java` | Spring wiring: `ReserveCreditService` and `ReleaseCreditService` beans. |

### Test adapters

| File | Purpose |
|------|---------|
| `adapter/outbound/ConfigurableCreditLimitPort.java` | Per-customer configurable available credit (defaults to zero). |
| `adapter/outbound/InMemoryCreditReservationRepository.java` | HashMap-backed repository for tests. |
| `adapter/outbound/RecordingCreditReservationEventPublisher.java` | Captures published events for assertion. |

### Test configuration and IT

| File | Change |
|------|--------|
| `integration/CreditServiceTestConfiguration.java` | New — wires in-memory adapters for context load tests. |
| `integration/CreditServiceApplicationIT.java` | Added `@Import(CreditServiceTestConfiguration.class)`. |
| `unit/ArchitectureTest.java` | Added two rules: domain and application must not depend on Avro/Kafka. |

---

## Reservation Logic

**Approval decision**:
```
available = creditLimitPort.availableCredit(customerId)
if available.canCover(requested)  →  APPROVED   (available.amount ≥ requested.amount, same currency)
else                               →  REJECTED
```

**Outcome decision**:
| Condition | Outcome | Domain factory |
|-----------|---------|----------------|
| `requested ≤ available` | APPROVED | `CreditReservation.approved()` |
| `requested > available` | REJECTED | `CreditReservation.rejected()` |

Both outcomes are persisted and publish a domain event.

**OPEN QUESTION**: Currency mismatch — if the requested currency differs from the credit
limit currency the comparison behaviour is undefined. The JPA adapter implementation must
define the currency comparison strategy before go-live.

---

## Release Decision for REJECTED Reservations

**Decision**: Releasing a REJECTED credit reservation is a **no-op** — no state change is
persisted and no `CreditReleasedDomainEvent` is published.

**Rationale**: A REJECTED reservation represents a credit denial; it never held any credit.
The domain model's `CreditReservation.release()` throws `IllegalArgumentException` for
REJECTED reservations. The application service guards against this by checking status first,
consistent with the principle that no-op compensation is the correct saga behaviour when no
resource was ever allocated.

**OPEN QUESTION**: Confirm with orchestrator-service that the saga compensation path does not
expect `CreditReleased` as a follow-up to `CreditRejected`. If the orchestrator relies on
`CreditReleased` as a completion signal even for the REJECTED case, this decision must be
revisited in ARB-014 / saga wiring.

---

## Idempotency

`ReleaseCreditService.release()` is idempotent:
- If the reservation status is already `RELEASED`, the method returns immediately without
  persisting or publishing an event.
- The second call returns the same `ReleaseCreditResult` as the first.

---

## Events

All three domain events are pure Java records — no Avro, no Kafka.

| Event | Fields | When |
|-------|--------|------|
| `CreditApprovedDomainEvent` | reservationId, orderId, customerId, amount | Sufficient credit |
| `CreditRejectedDomainEvent` | reservationId, orderId, customerId, amount | Insufficient credit |
| `CreditReleasedDomainEvent` | reservationId, orderId | APPROVED → RELEASED |

`customerId` and `amount` are included in the approve/reject events so that downstream
consumers (e.g., orchestrator-service) can route saga compensation without needing to reload
the reservation from the credit-service database.

---

## Open Questions

1. **REJECTED release and saga compensation**: Confirm orchestrator-service does not expect
   `CreditReleased` after `CreditRejected` (see Release Decision above).

2. **Currency mismatch**: When the requested currency differs from the credit limit currency,
   the `availableCredit ≥ requested` comparison is invalid. Must be resolved before the JPA
   adapter is implemented.

3. **`CreditLimitPort` implementation**: The port has no storage yet. The JPA adapter will
   need to query a `credit_limits` or `credit_accounts` table. Locking strategy (optimistic
   vs pessimistic) for concurrent reservations must be chosen before the JPA phase.

4. **Reservation not found on release**: `ReleaseCreditService` throws `IllegalArgumentException`.
   Once a Kafka consumer or REST adapter is added, this should map to a typed problem code
   (similar to `OrderProblemCode` in order-service).

5. **Duplicate reserve commands**: `ReserveCreditService` does not check for an existing
   reservation with the same `creditReservationId` before saving. If the orchestrator retries
   the reserve command, a second reservation would overwrite the first. An idempotency check
   should be added at the Kafka consumer layer.

6. **Transactionality**: Application services will become `@Transactional` when JPA persistence
   is introduced. DB + Kafka consistency will be handled by the Outbox pattern (ADR-0005).
