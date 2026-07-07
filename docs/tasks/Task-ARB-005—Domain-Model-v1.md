Task: ARB-005 — Domain Model v1

Status:
[PLANNED]

Owner:
Clio

Context:
ARB-003 Architecture Skeleton is DONE.
ARB-004 Platform Foundation is DONE.
ARB-004B Native Image / Spring AOT Technical Variant is DONE.

Goal:
Create the first pure Java domain model for UC-01 Corporate Bulk Order.

Scope:
Domain packages only.

Modules in scope:
- server/order-service
- server/inventory-service
- server/credit-service
- server/orchestrator-service

Source of truth:
- docs/okf/UC-01-corporate-bulk-order.md
- docs/rf/RF-UC-01-corporate-bulk-order.md
- docs/rnf/RNF-UC-01-saga-runtime.md
- docs/test-cases/
- docs/adr/ADR-0007-spring-aot-graalvm-native-image.md
- server/platform

Domain decisions:
- User = authenticated person from Keycloak.
- Customer = B2B buying company.
- Tenant = out of scope for v1; assume single tenant.
- Order belongs to Customer.
- Order is submitted by User.
- No gateway/BFF for now.
- Orchestrator is internal saga coordinator, not public gateway.

Required domain concepts:

order-service:
- UserId
- CustomerId
- OrderId
- Sku
- Quantity
- Money
- OrderLine
- OrderStatus
- CancellationReason
- Order

Expected Order statuses:
- PENDING
- CONFIRMED
- PARTIALLY_CONFIRMED
- CANCELLED
- AWAITING_CUSTOMER_DECISION

Expected cancellation reasons:
- CUSTOMER_CANCELLED
- CUSTOMER_DEFERRED
- INSUFFICIENT_CREDIT
- SYSTEM_TIMEOUT

inventory-service:
- StockReservationId
- WarehouseId
- StockReservationStatus
- StockReservationLine
- StockReservation

Expected StockReservation statuses:
- RESERVED
- PARTIALLY_RESERVED
- REJECTED
- RELEASED

credit-service:
- CreditReservationId
- CreditReservationStatus
- CreditReservation

Expected CreditReservation statuses:
- APPROVED
- REJECTED
- RELEASED

orchestrator-service:
- SagaId
- SagaStatus
- SagaStep
- CustomerDecision
- CompensationAction

Expected Saga statuses:
- STARTED
- AWAITING_CUSTOMER_DECISION
- COMPLETED
- CANCELLED
- FAILED_COMPENSATION

Expected CustomerDecision values:
- ACCEPT_PARTIAL
- WAIT_BACKORDER
- CANCEL_ORDER

Core invariants:
- Order must have at least one line.
- Order quantity must be positive.
- Money amount must be zero or positive.
- CONFIRMED requires all lines confirmed/reserved.
- PARTIALLY_CONFIRMED requires at least one confirmed line and at least one unconfirmed/backorder/cancelled line.
- CANCELLED requires CancellationReason.
- AWAITING_CUSTOMER_DECISION requires partial availability.
- Credit must not be consumed for CANCELLED orders.
- ReleaseStock must be modeled as idempotent at the domain level where applicable.
- CustomerDecision must be valid only when saga/order is awaiting customer decision.

Implementation constraints:
- Pure Java only.
- Prefer records and small immutable objects.
- Use platform validation primitives where useful.
- Use platform correlation/id primitives only if appropriate.
- No Spring annotations.
- No JPA annotations.
- No Kafka.
- No Avro.
- No REST controllers.
- No repositories.
- No services/application use cases yet.
- No database migrations.
- No RuntimeHints yet unless absolutely necessary.
- GraalVM Native Image compatibility must be preserved.
- Avoid reflection, dynamic proxies, Class.forName, runtime scanning.

Tests:
Create unit tests for:
- valid Order creation
- Order rejects empty lines
- Order rejects invalid quantities
- Order cancellation requires reason
- Order confirmed transition
- Order partial confirmation transition
- Order awaiting customer decision transition
- StockReservation full reserved
- StockReservation partial reserved
- StockReservation release is idempotent
- CreditReservation approved/rejected behavior
- SagaStatus and CustomerDecision transition rules

Documentation:
Create:
- docs/implementation/ARB-005-domain-model.md

Update if needed:
- server/order-service/README.md
- server/inventory-service/README.md
- server/credit-service/README.md
- server/orchestrator-service/README.md
- docs/okf/index.md

Acceptance criteria:
- All new domain types are pure Java.
- All public types have Javadocs.
- All packages have package-info.java.
- Unit tests pass without Docker, Kafka, Postgres, Keycloak, or Spring context.
- No infrastructure annotations are introduced.
- No Avro/JPA/Kafka/REST code is introduced.
- Native Image compatibility guardrail is respected.
- Implementation note documents created concepts, invariants, tests, and open questions.

After completion:
- Report created files.
- Report tests run.
- Report open questions.
- Do not start ARB-006.