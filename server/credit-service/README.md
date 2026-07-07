# credit-service

Validates and reserves B2B credit limits for corporate buyers.

## Responsibility

- Consumes `CreditReservationRequestedEvent` from Kafka.
- Checks available credit limit for the buyer's organization.
- Publishes `CreditReservedEvent` or `CreditReservationDeniedEvent`.
- Executes compensation (credit release) when the saga is rolled back.

## Hexagonal Package Structure

```
com.arbitrier.credit/
├── domain/
│   ├── CreditAccount.java               # Aggregate root (placeholder)
│   ├── CreditLimit.java                 # Value object (placeholder)
│   └── event/
│       ├── CreditReservedEvent.java              # Domain event (placeholder)
│       └── CreditReservationDeniedEvent.java     # Domain event (placeholder)
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── ReserveCreditUseCase.java          # Input port (placeholder)
│   │   │   └── ReleaseCreditUseCase.java          # Input port — compensation (placeholder)
│   │   └── out/
│   │       ├── CreditAccountRepository.java       # Output port (placeholder)
│   │       └── CreditEventPublisher.java          # Output port (placeholder)
│   └── service/
│       ├── ReserveCreditService.java              # Use-case impl (placeholder)
│       └── ReleaseCreditService.java              # Use-case impl (placeholder)
├── adapter/
│   ├── in/
│   │   └── kafka/
│   │       └── CreditReservationKafkaConsumerAdapter.java   # Kafka consumer (placeholder)
│   └── out/
│       ├── jpa/
│       │   └── CreditAccountJpaAdapter.java       # JPA impl (placeholder)
│       └── kafka/
│           └── CreditEventKafkaProducerAdapter.java         # Kafka producer (placeholder)
└── config/
    └── CreditServiceConfig.java
```

## Dependencies

- `server/platform`
- `server/contracts` — Avro schemas for credit events
- PostgreSQL (schema: `credit_service`)
- Kafka topics: `credit.reservation.requested.v1`, `credit.reserved.v1`, `credit.reservation.denied.v1`

## Status

`ARB-001` — Structure placeholder. No business logic implemented.
