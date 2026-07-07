# orchestrator-service

The saga coordinator. Drives the UC-01 Corporate Bulk Order saga from start to terminal state.

## Responsibility

- Listens for `OrderPlacedEvent` to start a new saga instance.
- Orchestrates the sequence: credit reservation → inventory reservation → (optional) human decision.
- Manages saga state machine transitions.
- Issues compensation commands when a step fails or is rejected.
- Notifies `order-service` of the final outcome (`CONFIRMED`, `PARTIALLY_CONFIRMED`, `CANCELLED`).

## Saga State Machine (UC-01)

```
STARTED
  └─► CREDIT_RESERVATION_REQUESTED
        ├─► CREDIT_RESERVED
        │     └─► INVENTORY_RESERVATION_REQUESTED
        │           ├─► INVENTORY_FULLY_RESERVED ──────────────────► CONFIRMED
        │           ├─► INVENTORY_PARTIALLY_RESERVED
        │           │     └─► AWAITING_HUMAN_DECISION
        │           │           ├─► HUMAN_ACCEPTED ────────────────► PARTIALLY_CONFIRMED
        │           │           └─► HUMAN_REJECTED
        │           │                 └─► (compensate credit + inventory) ──► CANCELLED
        │           └─► INVENTORY_RESERVATION_FAILED
        │                 └─► (compensate credit) ─────────────────► CANCELLED
        └─► CREDIT_RESERVATION_DENIED ───────────────────────────────► CANCELLED
```

## Hexagonal Package Structure

```
com.arbitrier.orchestrator/
├── domain/
│   ├── SagaInstance.java                # Aggregate root (placeholder)
│   ├── SagaStatus.java                  # Enum of all saga states (placeholder)
│   └── event/
│       └── SagaCompletedEvent.java      # Domain event (placeholder)
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   └── StartBulkOrderSagaUseCase.java    # Input port (placeholder)
│   │   └── out/
│   │       ├── SagaRepository.java               # Output port (placeholder)
│   │       └── SagaCommandPublisher.java          # Output port (placeholder)
│   └── service/
│       └── BulkOrderSagaOrchestrationService.java # Use-case impl (placeholder)
├── adapter/
│   ├── in/
│   │   └── kafka/
│   │       └── SagaEventKafkaConsumerAdapter.java # Kafka consumer (placeholder)
│   └── out/
│       ├── jpa/
│       │   └── SagaJpaAdapter.java               # JPA impl (placeholder)
│       └── kafka/
│           └── SagaCommandKafkaProducerAdapter.java # Kafka producer (placeholder)
└── config/
    └── OrchestratorServiceConfig.java
```

## Dependencies

- `server/platform`
- `server/contracts` — all saga event Avro schemas
- PostgreSQL (schema: `orchestrator_service`)
- Kafka: consumes from order, credit, inventory topics; produces command events

## Status

`ARB-001` — Structure placeholder. No business logic implemented.
