# inventory-service

Manages stock levels and processes inventory reservation requests from the saga.

## Responsibility

- Consumes `InventoryReservationRequestedEvent` from Kafka.
- Attempts to reserve the requested quantities.
- Publishes `InventoryReservedEvent` (full or partial) or `InventoryReservationFailedEvent`.
- Executes compensation (stock release) when the saga is rolled back.

## Hexagonal Package Structure

```
com.arbitrier.inventory/
├── domain/
│   ├── InventoryItem.java               # Aggregate root (placeholder)
│   ├── Reservation.java                 # Entity (placeholder)
│   └── event/
│       ├── InventoryReservedEvent.java          # Domain event (placeholder)
│       └── InventoryReservationFailedEvent.java  # Domain event (placeholder)
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── ReserveInventoryUseCase.java      # Input port (placeholder)
│   │   │   └── ReleaseInventoryUseCase.java      # Input port — compensation (placeholder)
│   │   └── out/
│   │       ├── InventoryRepository.java          # Output port (placeholder)
│   │       └── InventoryEventPublisher.java      # Output port (placeholder)
│   └── service/
│       ├── ReserveInventoryService.java          # Use-case impl (placeholder)
│       └── ReleaseInventoryService.java          # Use-case impl (placeholder)
├── adapter/
│   ├── in/
│   │   └── kafka/
│   │       └── InventoryReservationKafkaConsumerAdapter.java  # Kafka consumer (placeholder)
│   └── out/
│       ├── jpa/
│       │   └── InventoryJpaAdapter.java          # JPA impl (placeholder)
│       └── kafka/
│           └── InventoryEventKafkaProducerAdapter.java        # Kafka producer (placeholder)
└── config/
    └── InventoryServiceConfig.java
```

## Dependencies

- `server/platform`
- `server/contracts` — Avro schemas for reservation events
- PostgreSQL (schema: `inventory_service`)
- Kafka topics: `inventory.reservation.requested.v1`, `inventory.reserved.v1`, `inventory.reservation.failed.v1`

## Status

`ARB-001` — Structure placeholder. No business logic implemented.
