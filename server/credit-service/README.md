# credit-service

Validates and reserves B2B credit limits for corporate buyers.

## Responsibility

- Receives credit reservation and release commands (from orchestrator-service via Kafka — deferred).
- Checks available credit limit for the customer.
- Publishes domain events: `CreditApproved`, `CreditRejected`, `CreditReleased`.
- Executes compensation (credit release) idempotently when the saga is rolled back.

## Domain Model (ARB-005)

Pure-Java, zero-framework types in `com.arbitrier.credit.domain.model`:

| Type | Kind |
|------|------|
| `CreditReservationId` | record — unique reservation identifier |
| `Money` | record — non-negative amount with ISO-4217 currency (bounded-context copy) |
| `CreditReservationStatus` | enum — APPROVED, REJECTED, RELEASED |
| `CreditReservation` | final class — aggregate root; `release()` idempotent; throws on REJECTED |

## Application Slice (ARB-013)

### Inbound ports

| Interface | Location |
|-----------|----------|
| `ReserveCreditUseCase` | `application/port/inbound/` |
| `ReleaseCreditUseCase` | `application/port/inbound/` |

### Outbound ports

| Interface | Location | Status |
|-----------|----------|--------|
| `CreditReservationRepository` | `application/port/outbound/` | In-memory only (no JPA yet) |
| `CreditLimitPort` | `application/port/outbound/` | Configurable test adapter (no real store yet) |
| `CreditReservationEventPublisher` | `application/port/outbound/` | Recording only (no Kafka yet) |

### Domain events

| Event | When |
|-------|------|
| `CreditApprovedDomainEvent` | Requested amount ≤ available credit |
| `CreditRejectedDomainEvent` | Requested amount > available credit |
| `CreditReleasedDomainEvent` | APPROVED reservation released |

### Reservation outcome

```
available = creditLimitPort.availableCredit(customerId)
if requested ≤ available  →  APPROVED
else                       →  REJECTED
```

### Release idempotency

`ReleaseCreditUseCase` is idempotent. Re-releasing an already-RELEASED reservation is a no-op:
the same result is returned without persisting or publishing an event.

Releasing a REJECTED reservation is also a no-op (no credit was ever held — no event emitted).
See `ReleaseCreditService` Javadoc and `docs/implementation/ARB-013-credit-service.md` for
the documented decision.

### Test adapters (test tree)

| Class | Purpose |
|-------|---------|
| `InMemoryCreditReservationRepository` | HashMap-backed; no DB |
| `ConfigurableCreditLimitPort` | Per-customer configurable available credit |
| `RecordingCreditReservationEventPublisher` | Captures events for assertion |

## Build & Test

```bash
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/credit-service
```

Tests pass without Kafka, Postgres, Schema Registry, Keycloak, or Docker.

## Status

`ARB-013` — Application slice implemented: `ReserveCreditUseCase`, `ReleaseCreditUseCase`, domain events, test adapters. No JPA, Kafka, or REST yet.
`ARB-005` — Domain model implemented.
