Task: ARB-007 — Order Service Application Slice

Status:
[COMPLETE]

Owner:
Clio

Context:
ARB-005 Domain Model is DONE.
ARB-006 Domain Contracts is DONE.
Domain models must remain transport-agnostic and persistence-agnostic.
Avro generated classes are integration contracts only.
JPA entities are persistence models only.
REST DTOs are API models only.

Goal:
Implement the first vertical application slice for order-service:
submit a corporate bulk order and create an Order in PENDING state.

Primary use case:
UC-01.01 Submit Corporate Bulk Order.

Scope:
server/order-service only, plus minimal test support if needed.

In scope:
1. Inbound application port:
    - SubmitCorporateBulkOrderUseCase

2. Application command:
    - SubmitCorporateBulkOrderCommand
    - SubmitCorporateBulkOrderLineCommand

3. Application result:
    - SubmitCorporateBulkOrderResult

4. Application service:
    - SubmitCorporateBulkOrderService

5. Outbound ports:
    - OrderRepository
    - OrderEventPublisher

6. Domain event:
    - OrderCreatedDomainEvent
    - Keep it pure Java.
    - Do not use Avro generated classes here.

7. In-memory outbound adapters for tests/local slice:
    - InMemoryOrderRepository
    - RecordingOrderEventPublisher

8. Optional REST adapter:
    - SubmitCorporateBulkOrderController
    - CreateOrderRequest
    - CreateOrderLineRequest
    - CreateOrderResponse

Only add REST if it stays simple and does not require security integration yet.

Important architectural rule:
Do not use domain models as REST DTOs.
Do not use domain models as Avro messages.
Do not use domain models as JPA entities.

Mapping boundaries:
REST DTO -> Application Command -> Domain Model -> Domain Event -> Outbound Port

Out of scope:
- No KafkaTemplate.
- No Kafka producer implementation.
- No Avro mapper yet unless strictly needed for tests, preferably defer.
- No JPA entity.
- No real Postgres repository.
- No Flyway/Liquibase.
- No Keycloak integration.
- No pricing/catalog integration.
- No orchestrator integration.
- No inventory or credit calls.
- No saga orchestration.
- No native RuntimeHints.
- No tenant model.
- No gateway/BFF.

Functional behavior:
- Accept customerId, submittedByUserId, and one or more order lines.
- Validate at application boundary:
    - customerId required
    - submittedByUserId required
    - at least one line
    - sku required
    - quantity positive
- Create Order with status PENDING.
- Persist through OrderRepository port.
- Publish OrderCreatedDomainEvent through OrderEventPublisher port.
- Return orderId and status PENDING.

requestedTotal:
- Do not compute requestedTotal yet.
- Do not introduce pricing logic.
- Add an OPEN QUESTION in implementation note:
  requestedTotal source of truth remains unresolved from ARB-006.

Observability/logging:
- Use SLF4J logs in application service and controller if created.
- Use SafeLoggable/SafeRenderable where appropriate.
- Do not log raw customer/user sensitive data.
- Include orderId and correlationId if available.
- If correlationId is not yet part of command, mark OPEN QUESTION.

Testing:
- Unit test SubmitCorporateBulkOrderService happy path.
- Unit test invalid empty lines.
- Unit test invalid quantity.
- Unit test missing customerId.
- Unit test missing submittedByUserId.
- Unit test repository save is called.
- Unit test event publisher is called.
- Optional MVC test if REST controller is added.
- Tests must run without Docker, Kafka, Postgres, Keycloak, Schema Registry, or native image build.

Architecture:
- Application layer may depend on domain and application ports.
- Domain must not depend on application.
- Adapters may depend on application and domain.
- Domain must not depend on Spring/JPA/Kafka/Avro.
- Application must not depend on REST/JPA/Kafka/Avro generated classes.

Native Image compatibility:
- Avoid reflection, Class.forName, dynamic proxies, runtime scanning.
- If Spring MVC controller is added, document that Spring AOT will handle normal controller discovery later.
- Do not add RuntimeHints yet.

Documentation:
Create:
- docs/implementation/ARB-007-order-service-application-slice.md

Update if needed:
- server/order-service/README.md
- docs/okf/index.md
- docs/test-cases/TC-UC-01-001-create-pending-order.md

Acceptance Criteria:
- order-service compiles.
- Unit tests pass.
- Order can be created as PENDING through application service.
- OrderRepository port exists.
- OrderEventPublisher port exists.
- In-memory or recording test adapters exist.
- OrderCreatedDomainEvent exists and is pure Java.
- No Kafka runtime code.
- No Avro generated class usage.
- No JPA.
- No database migrations.
- Domain remains clean.
- Documentation captures requestedTotal and correlationId open questions.
- ARB-007 is ready for Deep review.

After completion:
- Report created files.
- Report tests run.
- Report open questions.
- Do not start ARB-008.
