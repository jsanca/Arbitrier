Task: ARB-012 — Inventory Service

Status:
[PLANNED]

Owner:
Clio

Context:
ARB-005 Domain Model is DONE.
ARB-006 Contracts is DONE.
ARB-011 Contracts & Messaging Foundation is DONE.

Inventory Service is a saga participant.
It does not orchestrate orders.
It only decides whether stock can be reserved, partially reserved, rejected, or released.

Goal:
Implement the first inventory-service application slice:
- Reserve stock for an order.
- Release stock idempotently.

Scope:
server/inventory-service only.

In scope:

1. Inbound application ports:
- ReserveStockUseCase
- ReleaseStockUseCase

2. Application commands:
- ReserveStockCommand
- ReserveStockLineCommand
- ReleaseStockCommand

3. Application results:
- ReserveStockResult
- ReleaseStockResult

4. Application services:
- ReserveStockService
- ReleaseStockService

5. Outbound ports:
- StockAvailabilityPort
- StockReservationRepository
- StockReservationEventPublisher

6. Domain events:
- StockReservedDomainEvent
- StockPartiallyReservedDomainEvent
- StockRejectedDomainEvent
- StockReleasedDomainEvent

7. Test adapters:
- InMemoryStockReservationRepository
- ConfigurableStockAvailabilityPort
- RecordingStockReservationEventPublisher

Functional behavior:

ReserveStockUseCase:
- Input:
    - orderId
    - reservationId
    - warehouseId
    - one or more lines
- Validate:
    - orderId required
    - reservationId required
    - warehouseId required
    - at least one line
    - sku required
    - quantity positive

Full reservation:
- If all requested quantities are available:
    - create StockReservation with RESERVED status
    - persist through StockReservationRepository
    - publish StockReservedDomainEvent
    - return RESERVED

Partial reservation:
- If at least one line can be reserved but not all:
    - create StockReservation with PARTIALLY_RESERVED status
    - persist through StockReservationRepository
    - publish StockPartiallyReservedDomainEvent
    - return PARTIALLY_RESERVED

Rejected reservation:
- If no requested line can be reserved:
    - create StockReservation with REJECTED status
    - persist through StockReservationRepository if consistent with current domain model
    - publish StockRejectedDomainEvent
    - return REJECTED

ReleaseStockUseCase:
- Load reservation by reservationId.
- If reservation is RESERVED or PARTIALLY_RESERVED:
    - call domain release()
    - persist RELEASED reservation
    - publish StockReleasedDomainEvent
    - return RELEASED
- If reservation is already RELEASED:
    - return RELEASED without corrupting state
    - avoid duplicate event unless explicitly documented
- If reservation is REJECTED:
    - choose smallest clean behavior and document it:
        - either no-op release, or
        - reject release as invalid
    - do not invent saga behavior

Important idempotency rule:
ReleaseStock must be idempotent.

Out of scope:
- No Kafka consumer.
- No Kafka producer.
- No Avro mapper.
- No JPA entity.
- No Postgres repository.
- No Flyway/Liquibase.
- No REST controller unless strictly needed.
- No warehouse optimization.
- No multi-warehouse split reservation.
- No saga orchestration.
- No credit logic.
- No order confirmation logic.
- No Keycloak/Spring Security integration.
- No native RuntimeHints.

Architecture rules:
- Domain remains pure Java.
- Application does not depend on REST/JPA/Kafka/Avro.
- Adapters may depend on application/domain.
- Domain events must be pure Java.
- No generated Avro classes in application/domain.

Logging:
- Use SLF4J in application services.
- Log only opaque IDs and counts.
- Do not log PII.
- Include reservationId/orderId/warehouseId where useful.

Native Image:
- Avoid reflection, Class.forName, dynamic proxies, runtime scanning.
- No RuntimeHints yet.

Tests:
- reserve full stock happy path
- reserve partial stock path
- reject when no stock available
- invalid empty lines
- invalid quantity
- missing orderId
- missing reservationId
- missing warehouseId
- release reserved reservation
- release partially reserved reservation
- release is idempotent
- release rejected reservation behavior documented and tested
- repository save called
- correct domain event published for each outcome

Documentation:
Create:
- docs/implementation/ARB-012-inventory-service.md

Update if needed:
- server/inventory-service/README.md
- docs/okf/index.md
- docs/test-cases/TC-UC-01-002-happy-path-full-confirmation.md
- docs/test-cases/TC-UC-01-003-partial-backorder-human-decision.md
- docs/test-cases/TC-UC-01-010-release-stock-idempotent.md

Acceptance Criteria:
- inventory-service compiles.
- tests pass without Docker, Kafka, Postgres, Keycloak, Schema Registry, or native image build.
- ReserveStockUseCase exists.
- ReleaseStockUseCase exists.
- StockAvailabilityPort exists.
- StockReservationRepository exists.
- StockReservationEventPublisher exists.
- Domain events are pure Java.
- No Kafka runtime code.
- No Avro generated class usage.
- No JPA.
- No database migrations.
- ReleaseStock behavior is explicitly idempotent.
- Documentation captures reservation/release decisions and open questions.
- ARB-012 is ready for Deep review.

After completion:
- Report created files.
- Report tests run.
- Report open questions.
- Do not start ARB-013.