Task: ARB-019 — Persistence Adapters

Status:
[COMPLETE]

Owner:
Clio

Context:

The Arbitrier business model, application services, inbound ports, and outbound
repository ports are already implemented.

Current repositories are in-memory test adapters.

ARB-019 introduces production persistence adapters using:

- PostgreSQL
- JPA
- Spring Data
- Optimistic locking

The domain model must remain persistence-agnostic.

Primary goal:

Implement JPA/PostgreSQL persistence adapters for:

- order-service
- inventory-service
- credit-service
- orchestrator-service

without leaking JPA annotations into domain aggregates.

Modules in scope:

- server/order-service
- server/inventory-service
- server/credit-service
- server/orchestrator-service

Supporting changes allowed:

- server/platform
- parent/module POM files
- documentation

Do not implement Kafka adapters.
Do not implement Outbox yet unless explicitly required for repository shape.

--------------------------------------------------

1. Persistence architecture

Use separate persistence models.

Preferred shape:

domain aggregate
↕ mapper
JPA entity
↕ Spring Data repository
Persistence adapter
↕ application repository port

Example:

Order
↕ OrderPersistenceMapper
OrderEntity
↕ SpringDataOrderRepository
JpaOrderRepositoryAdapter
implements OrderRepository

Do not annotate domain aggregates with:

- @Entity
- @Table
- @Id
- @Version
- @Embeddable

Domain remains pure Java.

--------------------------------------------------

2. Database schemas

Use PostgreSQL schemas:

- order_service
- inventory_service
- credit_service
- orchestrator_service

Each service must own its schema.

Do not create cross-schema foreign keys between bounded contexts.

Identifiers crossing service boundaries remain opaque values.

--------------------------------------------------

3. Order Service persistence

Implement JPA persistence for:

- Order
- OrderLine
- OrderStatus
- CustomerId
- UserId
- Money
- CancellationReason
- timestamps if present in the aggregate

Create:

- OrderEntity
- OrderLineEntity
- SpringDataOrderRepository
- JpaOrderRepositoryAdapter
- OrderPersistenceMapper

Requirements:

- preserve aggregate immutability at domain boundary;
- persist order lines as owned children;
- replace aggregate lines atomically on save if using merge/update mapping;
- no lazy-loading objects escape adapter;
- no JPA entity returned by application port.

Add optimistic locking:

- @Version on OrderEntity.

Handle concurrency conflict through a typed application/platform exception or
documented adapter exception mapping.

--------------------------------------------------

4. Inventory Service persistence

Implement JPA persistence for:

- StockReservation
- StockReservationLine
- StockAllocation
- StockReservationStatus
- WarehouseId

Model the multi-warehouse allocation introduced by ARB-017B.

Expected relationship:

StockReservationEntity
└── StockReservationLineEntity
└── StockAllocationEntity

Requirements:

- reservation owns lines;
- line owns allocations;
- reserved quantity remains derived in domain;
- do not persist a duplicated reservedQuantity unless required for indexing/reporting;
- if persisted, document it as denormalized and validate consistency.

Add optimistic locking:

- @Version on StockReservationEntity.

Release must load the complete allocation graph required by the domain.

--------------------------------------------------

5. Credit Service persistence

Implement JPA persistence for:

- CreditReservation
- CreditReservationStatus
- Money
- customer/order references

Create:

- CreditReservationEntity
- SpringDataCreditReservationRepository
- JpaCreditReservationRepositoryAdapter
- CreditReservationPersistenceMapper

Add optimistic locking:

- @Version on CreditReservationEntity.

Do not model the external credit limit source as JPA unless the existing port explicitly
requires it.

CreditLimitPort remains an outbound integration port.

--------------------------------------------------

6. Orchestrator Service persistence

Implement JPA persistence for Saga.

Persist:

- sagaId
- orderId
- customerId
- status
- currentStep
- customerDecision
- stockReservationId
- creditReservationId
- retry/attempt-related data only if currently part of the aggregate
- timestamps if part of the model

Create:

- SagaEntity
- SpringDataSagaRepository
- JpaSagaRepositoryAdapter
- SagaPersistenceMapper

Add optimistic locking:

- @Version on SagaEntity.

This repository is the source of truth for saga state.

Do not persist arbitrary workflow blobs or serialized aggregate JSON.

Use explicit columns.

--------------------------------------------------

7. Optimistic locking policy

Use @Version on aggregate root entities.

Create a consistent adapter-level strategy for:

- stale update detected;
- concurrent saga transition;
- concurrent reservation release;
- duplicate save attempt.

Preferred result:

OptimisticLockingFailureException
↓ adapter mapping
typed Arbitrier problem/exception

Do not leak raw JPA/Spring exceptions outside the adapter boundary.

If the final typed problem-code model is not ready, introduce the smallest clean mapping
and document the remaining platform integration.

--------------------------------------------------

8. Typed persistence errors

Introduce or use typed problems for:

- ORDER_NOT_FOUND
- STOCK_RESERVATION_NOT_FOUND
- CREDIT_RESERVATION_NOT_FOUND
- SAGA_NOT_FOUND
- OPTIMISTIC_LOCK_CONFLICT
- PERSISTENCE_FAILURE

Align with the existing ProblemCode / ApplicationProblem model.

Do not continue using IllegalArgumentException for not-found paths at the persistence
adapter boundary.

Keep exception mapping framework-independent above the adapter.

--------------------------------------------------

9. Spring configuration

Add production configuration for JPA adapters.

Use explicit @Configuration and @Bean wiring when useful.

Avoid component scanning as a hidden dependency mechanism where existing project style
prefers explicit configuration.

Configuration should be conditional on datasource/JPA availability if context-load tests
must still run without PostgreSQL.

Document the selected approach.

--------------------------------------------------

10. Transactions

Add @Transactional at the application/persistence boundary where appropriate.

Preferred:

- application service transaction boundary;
- repository save within transaction;
- no Kafka publication inside a plain DB transaction.

Important:

DB + Kafka consistency remains deferred to ARB-021 Outbox / Inbox Foundation.

Do not pretend @Transactional makes PostgreSQL + Kafka atomic.

--------------------------------------------------

11. Query behavior

Repository adapters must support the existing ports exactly.

Do not add generic CRUD interfaces to application ports.

Repositories should expose only business-required operations.

Avoid returning Page<Entity> or Spring Data types through application ports.

--------------------------------------------------

12. Mappers

Persistence mappers must:

- reconstruct valid domain aggregates;
- preserve value objects;
- reject corrupt persisted state;
- avoid silently defaulting invalid values;
- contain no business decisions beyond reconstruction.

Prefer explicit mapping over reflection-based mapping libraries.

Do not introduce MapStruct unless there is a clear benefit and native-image implications
are addressed.

--------------------------------------------------

13. Testing

Add:

A. Mapper tests
- domain → entity
- entity → domain
- full aggregate round trip
- invalid/corrupt entity handling

B. Repository adapter tests
Use PostgreSQL Testcontainers if compatible with the project test strategy.

Cover:

Order:
- save/load aggregate with lines
- update status
- optimistic-lock conflict

Inventory:
- save/load reservation
- multi-warehouse allocations round trip
- release update
- optimistic-lock conflict

Credit:
- save/load reservation
- status update
- optimistic-lock conflict

Saga:
- save/load complete saga state
- semantic transition persistence
- concurrent update conflict

C. Architecture tests
- domain has no JPA imports
- application has no Spring Data imports
- adapter may depend on JPA/Spring Data
- JPA entities stay inside outbound persistence adapter packages

Tests should not require the manually started local Docker Compose stack.
Prefer Testcontainers for adapter integration tests.

--------------------------------------------------

14. Native Image / AOT

Review JPA/Hibernate implications for GraalVM.

Document:

- entity reflection requirements;
- proxy/lazy-loading considerations;
- Spring AOT support;
- RuntimeHints only if required.

Prefer:

- eager aggregate reconstruction inside adapter;
- no lazy proxies crossing boundaries;
- explicit entity classes.

Do not perform a native build in this slice unless trivial.

--------------------------------------------------

15. Documentation

Create:

- docs/implementation/ARB-019-persistence-adapters.md

Create or update ADR if needed:

- persistence model separation;
- optimistic locking policy;
- transaction boundary policy.

Update:

- service READMEs;
- docs/okf/index.md;
- relevant RNFs;
- architecture diagrams if applicable.

Document:

- schema ownership;
- entity/domain separation;
- concurrency behavior;
- transaction boundaries;
- remaining Outbox dependency;
- test strategy.

--------------------------------------------------

Out of scope

- No Kafka consumers.
- No Kafka producers.
- No Avro mappers.
- No Outbox publisher.
- No Inbox implementation.
- No Flyway migrations unless a minimal schema is unavoidable for tests.
- No production seed data.
- No REST controllers.
- No UI work.
- No Kubernetes.
- No Terraform.
- No Resilience4j.
- No admin console.
- No ARB-020 work beyond coordination notes.

--------------------------------------------------

Acceptance Criteria

- All four services have production JPA repository adapters.
- Domain model remains free of JPA annotations.
- PostgreSQL schemas are service-owned and isolated.
- Optimistic locking exists on aggregate roots.
- Not-found and concurrency errors are typed.
- Multi-warehouse allocation persists correctly.
- Saga state persists and can be reconstructed fully.
- JPA exceptions do not leak outside adapters.
- Tests pass.
- Existing in-memory unit tests remain green.
- Documentation is complete.
- Ready for Deep review.

After completion:

Report:

- created and modified files;
- entities and relationships;
- repository adapters;
- transaction choices;
- optimistic-lock behavior;
- tests run;
- native-image concerns;
- open questions.

Do not start ARB-020.
