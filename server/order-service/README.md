# order-service

Owns the Order aggregate and exposes the saga entry point.

## Responsibility

- Accepts corporate bulk order submissions via REST (authenticated via JWT).
- Validates the submitting user's access to the customer account.
- Persists Order state (`PENDING → CONFIRMED | PARTIALLY_CONFIRMED | CANCELLED`).
- Publishes `OrderCreatedDomainEvent` to trigger the saga in `orchestrator-service`.

## Security (ARB-010)

All API endpoints require a valid JWT Bearer token issued by Keycloak.

| Rule | Detail |
|------|--------|
| User identity | Derived from JWT `sub` claim — never accepted from the request body |
| Customer access | `CustomerAccessPort.canSubmitOrder(userId, customerId)` must return `true`; otherwise 403 |
| Actuator | `/actuator/**` is open for Kubernetes health probes |
| JWT decoder | Configured via `spring.security.oauth2.resourceserver.jwt.issuer-uri` (deployment env var) |

In tests, `OrderServiceTestConfiguration` provides a mock `JwtDecoder`. Controller tests use `SecurityMockMvcRequestPostProcessors.jwt()`.

## Domain Model (ARB-005)

Pure-Java types in `com.arbitrier.order.domain.model`:

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

## Application Slice (ARB-007 / ARB-010)

`POST /api/orders` requires a Bearer JWT. `submittedByUserId` is derived from the JWT subject — it is not accepted in the request body.

### Inbound ports

| Interface | Location |
|-----------|----------|
| `SubmitCorporateBulkOrderUseCase` | `application/port/inbound/` |

### Outbound ports

| Interface | Location | Status |
|-----------|----------|--------|
| `OrderRepository` | `application/port/outbound/` | In-memory only (no JPA yet) |
| `OrderEventPublisher` | `application/port/outbound/` | `KafkaOrderEventPublisher` when `spring.kafka.bootstrap-servers` is set; `RecordingOrderEventPublisher` in tests |
| `CustomerAccessPort` | `application/port/outbound/` | `AllowAllCustomerAccessAdapter` placeholder |

### Problem codes

| Code | HTTP | When |
|------|------|------|
| `ORDER_ACCESS_DENIED` | 403 | `CustomerAccessPort` returns false |

### Kafka publisher adapter (ARB-011)

| Class | Location |
|-------|----------|
| `OrderCreatedAvroMapper` | `adapter/outbound/kafka/` |
| `KafkaOrderEventPublisher` | `adapter/outbound/kafka/` |
| `KafkaPublisherConfiguration` | `config/` |

`KafkaPublisherConfiguration` is activated only when `spring.kafka.bootstrap-servers` is set.
In tests, `RecordingOrderEventPublisher` (from `OrderServiceTestConfiguration`, `@Primary`) is used instead.

`requestedTotal` in `OrderCreated` Avro is emitted as `MoneyAmount("0","USD")` — a documented placeholder until pricing source of truth is resolved (ARB-006 open question).

The Avro value serializer must be configured for production (see `docs/implementation/ARB-011-contracts-messaging-foundation.md`).

### In-memory adapters (test tree)

`InMemoryOrderRepository`, `RecordingOrderEventPublisher` in `src/test/`. No Postgres or Kafka required to run tests.

## Build & Test

```bash
mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service
```

## Status

`ARB-011` — Kafka + Avro publishing foundation added. `KafkaOrderEventPublisher` maps `OrderCreatedDomainEvent` to `OrderCreated` Avro and publishes to `arbitrier.order.created.v1`. Avro serializer and Schema Registry deferred.
`ARB-010` — JWT security wired. `submittedByUserId` removed from request body. `CustomerAccessPort` enforced. Persistence (JPA) pending.
