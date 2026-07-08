# order-service

Owns the order aggregate and exposes the saga entry point.

## Responsibility

- Accepts bulk order submissions from the React client (REST).
- Persists order state and transitions (`PENDING → CONFIRMED | PARTIALLY_CONFIRMED | CANCELLED`).
- Publishes `OrderPlacedEvent` to Kafka to trigger the saga in `orchestrator-service`.
- Exposes a query endpoint for order status polling and SSE push.

## Hexagonal Package Structure

```
com.arbitrier.order/
├── domain/
│   ├── Order.java                    # Aggregate root (placeholder)
│   ├── OrderItem.java                # Value object (placeholder)
│   ├── OrderStatus.java              # Enum: PENDING, CONFIRMED, PARTIALLY_CONFIRMED, CANCELLED
│   └── event/
│       └── OrderPlacedEvent.java     # Domain event (placeholder)
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   └── PlaceBulkOrderUseCase.java   # Input port (placeholder)
│   │   └── out/
│   │       ├── OrderRepository.java         # Output port (placeholder)
│   │       └── OrderEventPublisher.java     # Output port (placeholder)
│   └── service/
│       └── PlaceBulkOrderService.java       # Use-case impl (placeholder)
├── adapter/
│   ├── in/
│   │   └── rest/
│   │       └── OrderRestAdapter.java        # REST controller (placeholder)
│   └── out/
│       ├── jpa/
│       │   └── OrderJpaAdapter.java         # JPA impl (placeholder)
│       └── kafka/
│           └── OrderKafkaProducerAdapter.java # Kafka producer (placeholder)
└── config/
    └── OrderServiceConfig.java
```

## Dependencies

- `server/platform` — security, observability, common exceptions
- `server/contracts` — Avro schema for `OrderPlacedEvent`
- PostgreSQL (schema: `order_service`)
- Kafka topic: `order.placed.v1`

## Domain Model (ARB-005)

Pure-Java, zero-framework types in `com.arbitrier.order.domain.model`:

| Type | Kind |
|------|------|
| `UserId` | record — authenticated user identifier |
| `CustomerId` | record — corporate buyer identifier |
| `OrderId` | record — unique order identifier |
| `Sku` | record — stock-keeping unit code |
| `Quantity` | record — positive integer line quantity |
| `Money` | record — non-negative amount with ISO-4217 currency |
| `OrderLine` | record — SKU + Quantity pair |
| `OrderStatus` | enum — PENDING, AWAITING_CUSTOMER_DECISION, CONFIRMED, PARTIALLY_CONFIRMED, CANCELLED |
| `CancellationReason` | enum — CUSTOMER_CANCELLED, CUSTOMER_DEFERRED, INSUFFICIENT_CREDIT, SYSTEM_TIMEOUT |
| `Order` | final class — aggregate root with immutable lifecycle transitions |

## Application Slice (ARB-007)

`POST /api/orders` is live as a REST endpoint (no auth yet — Keycloak integration is pending).

### Inbound ports

| Interface | Location |
|-----------|----------|
| `SubmitCorporateBulkOrderUseCase` | `application/port/inbound/` |

### Outbound ports

| Interface | Location | Status |
|-----------|----------|--------|
| `OrderRepository` | `application/port/outbound/` | In-memory only (no JPA yet) |
| `OrderEventPublisher` | `application/port/outbound/` | No-op / recording only (no Kafka yet) |

### In-memory adapters (test tree)

`InMemoryOrderRepository` and `RecordingOrderEventPublisher` live in `src/test/` and are used by unit and integration tests. Neither Postgres nor Kafka are required to run the test suite.

## Status

`ARB-007` — Application slice implemented. REST adapter active at `POST /api/orders`. Persistence (JPA) and event publishing (Kafka/Avro) are pending their own tasks.
