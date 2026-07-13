# ARB-014 — Saga Orchestrator Foundation

| Field  | Value       |
|--------|-------------|
| Task   | ARB-014     |
| Status | Implemented |
| Date   | 2026-07-08  |

## Summary

Saga orchestrator foundation for orchestrator-service: `StartSagaUseCase`, `AdvanceSagaUseCase`,
and `CompensateSagaUseCase` with domain event coverage and in-memory test adapters. No JPA, Kafka,
Avro, REST, or Docker required to run tests.

---

## Domain Model Changes

The following changes were applied to the existing domain model:

### `SagaStep` — added `ORDER_CREATED`

`ORDER_CREATED` is the initial step when a saga is started. It represents the trigger event
(an order was placed). The saga is subsequently advanced through steps by `AdvanceSagaUseCase`.

### `SagaStatus` — added `COMPENSATING`

`COMPENSATING` is a transient (non-terminal) status entered via `CompensateSagaUseCase`.
The saga stays COMPENSATING while compensation commands are being issued (ARB-016).
Terminal transitions from COMPENSATING are: `CANCELLED` (compensation succeeded) or
`FAILED_COMPENSATION` (compensation failed).

### `Saga` — added `customerId`, `advance()`, `compensate()`

| Change | Description |
|--------|-------------|
| `customerId` field | Saga now carries the customer identifier for routing to credit-service |
| `start(id, orderId, customerId)` | Factory now accepts customerId; initial step is `ORDER_CREATED` |
| `advance(SagaStep nextStep)` | General step transition — validates non-terminal and non-COMPENSATING |
| `compensate()` | Transitions to COMPENSATING — validates non-terminal and not already COMPENSATING |

---

## Files Created

### Domain events

| File | Purpose |
|------|---------|
| `domain/event/SagaStartedDomainEvent.java` | Emitted when a new saga is created (sagaId, orderId, customerId). |
| `domain/event/SagaAdvancedDomainEvent.java` | Emitted when a saga advances to a new step (sagaId, orderId, currentStep). |
| `domain/event/SagaCompensatedDomainEvent.java` | Emitted when a saga enters COMPENSATING status (sagaId, orderId). |

### Application — inbound ports

| File | Purpose |
|------|---------|
| `application/port/inbound/StartSagaUseCase.java` | Input port for starting a saga. |
| `application/port/inbound/AdvanceSagaUseCase.java` | Input port for advancing a saga step. |
| `application/port/inbound/CompensateSagaUseCase.java` | Input port for beginning compensation. |
| `application/port/inbound/StartSagaCommand.java` | Command: sagaId, orderId, customerId. |
| `application/port/inbound/AdvanceSagaCommand.java` | Command: sagaId, nextStep (SagaStep enum). |
| `application/port/inbound/CompensateSagaCommand.java` | Command: sagaId. |
| `application/port/inbound/StartSagaResult.java` | Result: sagaId. |
| `application/port/inbound/AdvanceSagaResult.java` | Result: sagaId, currentStep. |
| `application/port/inbound/CompensateSagaResult.java` | Result: sagaId. |

### Application — outbound ports

| File | Purpose |
|------|---------|
| `application/port/outbound/SagaRepository.java` | Persists and loads `Saga` aggregates. |
| `application/port/outbound/SagaEventPublisher.java` | Publishes all three saga domain events. |

### Application services

| File | Purpose |
|------|---------|
| `application/service/StartSagaService.java` | Implements `StartSagaUseCase`. |
| `application/service/AdvanceSagaService.java` | Implements `AdvanceSagaUseCase`. |
| `application/service/CompensateSagaService.java` | Implements `CompensateSagaUseCase`. |

### Configuration

| File | Purpose |
|------|---------|
| `config/OrchestratorServiceConfiguration.java` | Spring wiring; `@Bean` returns port interfaces. |

### Test adapters

| File | Purpose |
|------|---------|
| `adapter/outbound/InMemorySagaRepository.java` | HashMap-backed repository keyed by `SagaId`. |
| `adapter/outbound/RecordingSagaEventPublisher.java` | Captures published events for assertion. |

### Test configuration and IT

| File | Change |
|------|--------|
| `integration/OrchestratorServiceTestConfiguration.java` | New — wires in-memory adapters. |
| `integration/OrchestratorServiceApplicationIT.java` | Added `@Import(OrchestratorServiceTestConfiguration.class)`. |
| `unit/ArchitectureTest.java` | Added Avro/Kafka dependency rules for domain and application. |

---

## Service Behaviour

### `StartSagaService`

1. Parse `SagaId` from command.
2. Create `Saga.start(id, orderId, customerId)` — status `STARTED`, step `ORDER_CREATED`.
3. Persist via `SagaRepository`.
4. Publish `SagaStartedDomainEvent`.
5. Return `StartSagaResult(sagaId)`.

### `AdvanceSagaService`

1. Load saga or throw `IllegalArgumentException`.
2. Call `saga.advance(nextStep)` — domain validates non-terminal and non-COMPENSATING.
3. Persist updated saga.
4. Publish `SagaAdvancedDomainEvent(sagaId, orderId, currentStep)`.
5. Return `AdvanceSagaResult(sagaId, currentStep)`.

### `CompensateSagaService`

1. Load saga or throw `IllegalArgumentException`.
2. Call `saga.compensate()` — domain validates non-terminal and not already COMPENSATING.
3. Persist updated saga (status: COMPENSATING, step: unchanged).
4. Publish `SagaCompensatedDomainEvent(sagaId, orderId)`.
5. Return `CompensateSagaResult(sagaId)`.

---

## Open Questions

1. **Duplicate saga on start**: `StartSagaService` does not check for an existing saga with the
   same `sagaId`. If the Kafka consumer retries, a second call would overwrite the first. An
   idempotency check should be added at the Kafka consumer layer (ARB-015).

2. **Step validation**: `AdvanceSagaService` delegates step validity to the domain's
   `advance()` method, which only validates non-terminal and non-COMPENSATING. It does NOT
   enforce a specific step sequence. The sequencing logic (which step is valid next given
   the current state) belongs to the happy-path wiring in ARB-015.

3. **Saga not found**: All three services throw `IllegalArgumentException` for unknown IDs.
   Once Kafka consumers exist, this should map to a typed problem code or a dead-letter
   routing strategy.

4. **Transactionality**: Application services will become `@Transactional` when JPA persistence
   is introduced. DB + Kafka consistency will be handled by the Outbox pattern (ADR-0005).

5. **COMPENSATING to terminal**: The path from `COMPENSATING` to `CANCELLED` or
   `FAILED_COMPENSATION` is not yet wired. ARB-016 will add the compensation workflow.

6. **`customerId` in `CompensateSagaCommand`**: Compensation may need to know which customer's
   credit to release. The saga aggregate carries `customerId`, so the service can read it from
   the loaded aggregate without requiring it in the command. Confirm this is sufficient for
   ARB-016 compensation routing.
