# ARB-007 — Order Service Application Slice

| Field  | Value       |
|--------|-------------|
| Task   | ARB-007     |
| Status | Implemented |
| Date   | 2026-07-07  |

## Files Created

### Application layer (production)

| File | Kind |
|------|------|
| `application/port/inbound/SubmitCorporateBulkOrderUseCase.java` | Inbound port interface |
| `application/port/inbound/SubmitCorporateBulkOrderCommand.java` | Application command record |
| `application/port/inbound/SubmitCorporateBulkOrderLineCommand.java` | Application command line record |
| `application/port/inbound/SubmitCorporateBulkOrderResult.java` | Result record |
| `application/port/outbound/OrderRepository.java` | Outbound port interface |
| `application/port/outbound/OrderEventPublisher.java` | Outbound port interface |
| `application/service/SubmitCorporateBulkOrderService.java` | Use-case implementation |
| `domain/event/OrderCreatedDomainEvent.java` | Pure-Java domain event record |
| `config/OrderServiceConfiguration.java` | Spring @Configuration wiring |

### REST adapter (production)

| File | Kind |
|------|------|
| `adapter/inbound/rest/CreateOrderRequest.java` | REST request DTO |
| `adapter/inbound/rest/CreateOrderLineRequest.java` | REST request line DTO |
| `adapter/inbound/rest/CreateOrderResponse.java` | REST response DTO |
| `adapter/inbound/rest/SubmitCorporateBulkOrderController.java` | REST controller (POST /api/orders) |

### Test support

| File | Kind |
|------|------|
| `src/test/.../adapter/outbound/InMemoryOrderRepository.java` | In-memory port implementation |
| `src/test/.../adapter/outbound/RecordingOrderEventPublisher.java` | Capturing port implementation |
| `src/test/.../application/service/SubmitCorporateBulkOrderServiceTest.java` | 10 unit tests |

## Mapping Boundary

```
POST /api/orders
  → CreateOrderRequest (REST DTO)
  → SubmitCorporateBulkOrderCommand (Application command)
  → Order.create() (Domain aggregate)
  → OrderRepository.save() (Outbound port)
  → OrderCreatedDomainEvent (Domain event)
  → OrderEventPublisher.publish() (Outbound port)
  → SubmitCorporateBulkOrderResult
  → CreateOrderResponse (REST DTO)
```

## What Was Intentionally Not Implemented

| Area | Reason |
|------|--------|
| Kafka / KafkaTemplate | No Kafka runtime code until Avro adapters are implemented |
| Avro mapper for OrderCreated | Deferred; `OrderEventPublisher` implementation will map domain event to Avro |
| JPA entity and PostgreSQL repository | No persistence yet; in-memory adapter used for tests |
| Flyway migration | No schema yet |
| Keycloak authentication | RESOLVED in ARB-010 — Spring Security Resource Server wired; `submittedByUserId` derived from JWT subject |
| Pricing / requestedTotal | OPEN QUESTION — source of truth unresolved from ARB-006 |
| RuntimeHints | No native-hostile patterns introduced; Spring AOT handles controller discovery |
| Tenant model | Out of scope for v1 |

## Open Questions

- OPEN QUESTION: `requestedTotal` source of truth — the `OrderCreated` Avro contract carries this field but the domain Order does not. The implementing task for the Kafka adapter must resolve whether the application layer computes it from catalog data, order-service stores it when pricing is modeled, or a catalog service owns it. (See ARB-006 design note.)
- OPEN QUESTION: `correlationId` is not yet part of `SubmitCorporateBulkOrderCommand`. When platform correlation primitives are wired to the REST layer (e.g. via request headers or interceptors), `correlationId` should be propagated into the command and logged with every saga step.
- OPEN QUESTION: Exact endpoint URL and HTTP response shape subject to API design task.
- OPEN QUESTION: Idempotency key source for order submission (see TC-UC-01-011).
- RESOLVED (ARB-010): `submittedByUserId` was previously accepted from the request body as a placeholder. It is now derived exclusively from the JWT subject claim — the request DTO no longer contains this field.
