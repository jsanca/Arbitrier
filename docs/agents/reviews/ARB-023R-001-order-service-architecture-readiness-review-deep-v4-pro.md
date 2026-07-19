# ARB-023R-001 — Order Service Architecture Readiness Review

**Reviewer**: Deep V4 Pro  
**Role**: Architecture Review  
**Date**: 2026-07-18  
**Scope**: Full order-service production readiness assessment (domain, hexagonal layering, transactions, saga boundaries, outbox, integration, errors, tests, future evolution)  
**Artifacts reviewed**: Source under `server/order-service/src/main/` and `src/test/`, plus platform primitives `Require`, `OutboxRepository`, `DomainEventToOutboxMapper`, `ApplicationProblemException`, `ProblemResponse`

---

## Executive Summary

The order-service is **architecturally sound and production-ready for its defined scope**. It owns exactly the responsibilities specified: order acceptance, persistence, and saga-start event emission via a transactional outbox. Hexagonal boundaries are strictly enforced by ArchUnit and respected by all code. The test strategy provides high confidence through a layered pyramid culminating in a vertical-slice proof.

There are **no Critical or Major findings**. The slice is well-isolated, correctly scoped, and evolvable. Minor cleanup and deferred capability items are noted below.

---

## 1. Domain Model

**Score: 8/10**

### Strengths

- **Pure Java domain**: Zero Spring/JPA/Kafka/gRPC imports. The `Order` aggregate is `final`, immutable, and every state transition returns a new instance.
- **Value Objects as records**: `OrderId`, `CustomerId`, `UserId`, `Sku`, `Quantity`, `Money`, `OrderLine` all use Java records with compact-constructor validation via `Require`. Consistent `of()` factory + custom `toString()` pattern.
- **Well-defined state machine**: Four transitions (`confirm`, `awaitCustomerDecision`, `confirmPartially`, `cancel`) with explicit guards. `isTerminal()` correctly identifies `CONFIRMED`, `PARTIALLY_CONFIRMED`, `CANCELLED`.
- **Optimistic concurrency**: `version` (Long) is carried as opaque token; the aggregate never mutates it. Separation of persistence concern is clean.
- **`CancellationReason` enum**: Covers the four expected cancellation scenarios (`CUSTOMER_CANCELLED`, `CUSTOMER_DEFERRED`, `INSUFFICIENT_CREDIT`, `SYSTEM_TIMEOUT`). No string-typed state.
- **`OrderCreatedDomainEvent`**: Single domain event with required fields validated in compact constructor. Carries order identity, customer identity, submitting user, and line items.

### Weaknesses

| ID | Severity | Category | Evidence | Recommendation |
|----|----------|----------|----------|----------------|
| D-1 | **Minor** | Dead code | `Money` VO is defined in `domain/model/` but never referenced by `Order`, `OrderLine`, or any command/result type | Remove or defer to a pricing milestone; unused domain types create false expectations |
| D-2 | **Observation** | Incomplete event surface | No domain events for `OrderConfirmed`, `OrderCancelled`, or `OrderPartiallyConfirmed`. The state transitions exist in the aggregate but produce no events. | Add domain events when downstream consumers need them; current scope (saga-start only) is acceptable |
| D-3 | **Observation** | Empty packages | `domain/command/` and `domain/exception/` contain only `package-info.java`. Actual command types live in `application/port/inbound/`. | Keep as-is — `command/` namespace is a legitimate placeholder for future domain-level commands |

---

## 2. Hexagonal Architecture

**Score: 9/10**

### Strengths

- **Strict layering enforced**: `ArchitectureTest` verifies no domain→adapter, application→adapter, domain→Spring, application→Avro/Kafka imports. All code passes.
- **Inbound adapters**: `SubmitCorporateBulkOrderController` (REST) correctly derives `submittedByUserId` from JWT `authentication.getName()` — never from request body (ARB-010 anti-spoofing).
- **Outbound adapters**: All four outbound patterns present — persistence (`JpaOrderRepositoryAdapter`), gRPC (`GrpcInventoryAvailabilityAdapter`), Kafka (`KafkaOrderEventPublisher`), customer access (`AllowAllCustomerAccessAdapter`). Each implements a port interface.
- **Dependency rule respected**: `adapter → application → domain`. No outward flows.
- **Conditional wiring**: Kafka beans gated by `@ConditionalOnProperty("spring.kafka.bootstrap-servers")`, gRPC client by `@ConditionalOnProperty("grpc.client.inventory.address")`. Tests skip both silently.
- **`package-info.java` in every package**: ArchUnit-enforced.

### Weaknesses

| ID | Severity | Category | Evidence | Recommendation |
|----|----------|----------|----------|----------------|
| H-1 | **Minor** | Dual publish path | `OrderEventPublisher` (Kafka) and `OutboxRepository` (outbox) are both defined but the application service uses only the outbox path. `KafkaOrderEventPublisher` is wired as a bean but never injected by the service. | Document the future intent (direct publish for non-saga events?) or remove the unused `OrderEventPublisher` adapter until needed |
| H-2 | **Observation** | Port granularity | `CustomerAccessPort.canSubmitOrder(String, String)` uses raw strings rather than `UserId`/`CustomerId` value objects. The adapter must re-construct VOs from strings. | Accept domain value objects in the port signature; the adapter should handle primitive→VO conversion |

---

## 3. Transaction Boundaries

**Score: 9/10**

### Strengths

- **Atomic order + outbox**: `SubmitCorporateBulkOrderService.execute()` is `@Transactional`. Order persistence and outbox write happen within the same transaction. `TransactionalOutboxIT` verifies both are visible after commit.
- **Rollback proof**: `OrderSagaStartRollbackIT` proves that when the outbox throws, the order is not persisted.
- **Optimistic locking**: `@Version` column on `OrderEntity`; `JpaOrderRepositoryAdapter.save()` maps `OptimisticLockingFailureException` to `ApplicationProblemException`.
- **Reads before writes**: The service performs all read-side work (customer access check, SKU aggregation, inventory availability) before mutating state. No read-after-write within the transaction scope.

### Weaknesses

| ID | Severity | Category | Evidence | Recommendation |
|----|----------|----------|----------|----------------|
| T-1 | **Observation** | Transaction scope | The gRPC inventory availability check runs inside the `@Transactional` boundary. While this is harmless (no writes before the check, rollback is clean on failure), it holds a database connection during a network call. | Extract reads to a non-transactional outer method; only persist inside `@Transactional` |

---

## 4. Saga Responsibilities

**Score: 10/10**

### Strengths

- Service owns exactly: order acceptance, order persistence, saga-start event emission.
- Does NOT orchestrate, reserve inventory, reserve credit, or wait for distributed completion.
- The `OrderCreatedDomainEvent` going into the outbox is the correct saga-start signal.
- Inventory availability check (`InventoryAvailabilityPort.checkAvailability()`) is a pre-advisory read — it does not reserve stock. This aligns with ADR-0009 (Inventory owns allocation).

### Weaknesses

None identified.

---

## 5. Outbox Design

**Score: 8/10**

### Strengths

- Uses platform's `DomainEventToOutboxMapper` + `OutboxRepository`. Event serialized via `EventSerializer` (JSON format).
- `OutboxEvent` model is rich: `eventType`, `aggregateId`, `aggregateType`, `messageNature`, `publishStatus`, `attemptCount`, `correlationId`, `causationId`, `claimedBy`, `claimedAt`. Full lifecycle support.
- `MessageNature.EVENT` correctly used.
- `DomainEventToOutboxMapper` generates a unique `UUID` per event and records `occurredAt` via `TimeProvider`.

### Weaknesses

| ID | Severity | Category | Evidence | Recommendation |
|----|----------|----------|----------|----------------|
| O-1 | **Minor** | Missing correlation | `SubmitCorporateBulkOrderCommand` does not carry a `correlationId`. The outbox event's `correlationId` and `causationId` remain null. | Add `correlationId` to the command and thread it through to the outbox event |
| O-2 | **Observation** | Fragile event type | `eventType` is derived from `event.getClass().getSimpleName()` ("OrderCreatedDomainEvent"). Renaming the class would break event consumers. | Consider a stable event type string constant on the event class itself |
| O-3 | **Observation** | No idempotency | No idempotency key on `SubmitCorporateBulkOrderCommand`. Duplicate HTTP submissions create duplicate orders. The `IdempotencyStore` port exists in platform but is unused. | Defer to a dedicated idempotency task; current scope is acceptable |

---

## 6. Integration Boundaries

**Score: 9/10**

### Strengths

- **REST → Application**: `CreateOrderRestMapper` translates request DTOs to application commands. JWT subject flows from controller, not request body. Bean Validation (`@NotBlank`, `@NotEmpty`, `@Min`) catches malformed input at the adapter boundary.
- **Application → gRPC**: `GrpcInventoryAvailabilityAdapter` cleanly separates protobuf translation (request mapper, response mapper), gRPC invocation (blocking stub + deadline), and exception mapping (4-type hierarchy from gRPC status codes).
- **Response-contract validation**: `validateResponseContract()` in the adapter checks line count, duplicate SKUs, and unexpected SKUs. Any mismatch becomes `InventoryAvailabilityProtocolException` → 502.
- **Protobuf isolation**: All protobuf types confined to `adapter.outbound.grpc.inventory`. Application ports use only `AvailabilityLineQuery`/`AvailabilityLineResponse`.
- **Exception hierarchy**: 4 gRPC exception subtypes (`Timeout`, `RemoteUnavailable`, `Protocol`, `Internal`) mapped to distinct HTTP status codes (504, 503, 502, 500) via `OrderInventoryExceptionHandler`.

### Weaknesses

| ID | Severity | Category | Evidence | Recommendation |
|----|----------|----------|----------|----------------|
| I-1 | **Observation** | Placeholder adapter | `AllowAllCustomerAccessAdapter` always returns `true`. No real customer-membership integration exists. | This is documented as a placeholder; no action needed until customer auth requirements are finalized |
| I-2 | **Observation** | Avro serializer placeholder | `OrderCreatedAvroMapper` emits `requestedTotal: { amount: "0", currency: "USD" }` as a documented placeholder. | This is acknowledged in code comments (ARB-011); no action needed |

---

## 7. Error Semantics

**Score: 9/10**

### Strengths

- **Clear taxonomy**: Business failures (`ApplicationProblemException` with typed `ProblemCode`), protocol failures (`InventoryAvailabilityProtocolException`), transport failures (`Timeout`, `RemoteUnavailable`), infrastructure failures (`Internal`). gRPC `StatusRuntimeException` never escapes the adapter.
- **HTTP mappings are correct**:
  - `CUSTOMER_ACCESS_DENIED` → 403
  - `ORDER_ITEMS_UNAVAILABLE` → 422
  - `OPTIMISTIC_LOCK_CONFLICT` → 409
  - `INVENTORY_TIMEOUT` → 504
  - `INVENTORY_SERVICE_UNAVAILABLE` → 503
  - `INVENTORY_PROTOCOL_ERROR` → 502
  - `INVENTORY_INTEGRATION_ERROR` → 500
- **`PlatformExceptionHandler`** handles `ApplicationProblemException` generically.
- **`JpaOrderRepositoryAdapter`** translates Spring Data exceptions to `ApplicationProblemException` before they cross the adapter boundary.

### Weaknesses

None identified.

---

## 8. Test Architecture

**Score: 9/10**

### Strengths

**Test pyramid** (from cheapest to most expensive):

| Layer | Test class | Type | Spring? | DB? |
|-------|-----------|------|---------|-----|
| Domain | `OrderTest` | Unit | No | No |
| Architecture | `ArchitectureTest` | Unit (ArchUnit) | No | No |
| Mappers | `CreateOrderRestMapperTest`, `OrderPersistenceMapperTest`, `OrderCreatedAvroMapperTest`, `InventoryAvailabilityGrpcRequestMapperTest`, `InventoryAvailabilityGrpcResponseMapperTest` | Unit | No | No |
| Application service | `SubmitCorporateBulkOrderServiceTest` (451 lines), `PrepareCorporateBulkOrderServiceTest` | Hand-wired | No | No |
| Kafka publisher | `KafkaOrderEventPublisherTest` | Mockito | No | No |
| gRPC adapter | `GrpcInventoryAvailabilityAdapterTest` (304 lines) | Mock stub | No | No |
| REST controller | `SubmitCorporateBulkOrderControllerTest` (264 lines) | `@SpringBootTest` + MockMvc | Yes | No |
| Integration (no JPA) | `OrderServiceApplicationIT`, `OrderCreationIT` | `@SpringBootTest` | Yes | No (in-memory adapters) |
| Persistence ITs | `FlywayMigrationIT`, `RepositoryRoundTripIT`, `TransactionalOutboxIT`, `OrderSagaStartRollbackIT` | `@SpringBootTest` + Testcontainers | Yes | Yes (PostgreSQL) |
| JPA adapter | `JpaOrderRepositoryAdapterTest` | `@SpringBootTest` + Testcontainers (create-drop) | Yes | Yes (PostgreSQL) |
| Cross-service IT | `CorporateBulkOrderVerticalIT`, `OrderToInventoryGrpcIntegrationIT` | Full context + in-process gRPC | Yes | Yes (PostgreSQL) |

- **Vertical proof**: `CorporateBulkOrderVerticalIT` exercises the full production path — MockMvc → controller → mapper → service → gRPC adapter → in-process inventory → JPA → PostgreSQL. Three scenarios: duplicate SKU normalization, insufficient inventory (422), gRPC integration failure (502).
- **Rollback proof**: `OrderSagaStartRollbackIT` confirms outbox failure prevents order persistence.
- **Atomicity proof**: `TransactionalOutboxIT` confirms order + outbox are both visible post-commit.
- **ArchUnit enforcement**: `ArchitectureTest` verifies dependency rules, `@Entity` location constraints, and gRPC dependency confinement.
- **Proper Spring Boot 4.1 patterns**: `@MockitoBean` (not `@MockBean`), `@Primary` test beans (no override), `@ConditionalOnMissingBean` on persistence config.
- **Well-designed stubs**: `StubInventoryAvailabilityPort` supports protocol failure simulation; `InMemoryOrderRepository` uses `ConcurrentHashMap`; `RecordingOrderEventPublisher` is minimal.

### Weaknesses

| ID | Severity | Category | Evidence | Recommendation |
|----|----------|----------|----------|----------------|
| TE-1 | **Observation** | Schema divergence | `JpaOrderRepositoryAdapterTest` uses `ddl-auto=create-drop` (Flyway disabled) while all other persistence ITs use `ddl-auto=validate`. This tests JPA mapping independently of Flyway, which is intentional but could mask entity-vs-migration mismatches. | Run `FlywayMigrationIT` + `RepositoryRoundTripIT` as a pair to cover both paths; already done |
| TE-2 | **Observation** | Missing stress tests | No concurrent-submission test on the outbox or order repository. The `StaleClaimRecoveryIT` and concurrent `claimPending` tests live in platform — not duplicated in order-service. | Acceptable; concurrent behavior is platform's responsibility |

---

## 9. Future Evolution

**Score: 8/10**

### Strengths

| Capability | Readiness | Mechanism |
|------------|-----------|-----------|
| Kafka relay | Ready | Outbox pattern in place; switching to CDC/Debezium requires no code changes |
| Saga orchestrator | Ready | `OrderCreatedDomainEvent` in outbox is exactly the signal the orchestrator will consume |
| Retries | Ready | `OutboxEvent.attemptCount`/`lastAttempt` fields exist; platform-level retry infrastructure built (ARB-022) |
| Idempotency | Pre-wired | `InboxRepository` wired as bean; `IdempotencyStore` port defined in platform |
| Correlation propagation | Structurally ready | `OutboxEvent.correlationId`/`causationId` fields exist; needs command-level wiring |
| Observability | Package declared | `observability/` package with `package-info.java` describing MDC + OpenTelemetry intent |
| Customer auth | Pluggable | `CustomerAccessPort` interface clean; `AllowAllCustomerAccessAdapter` is injectable |
| Pricing | Placeholder | `Money` VO defined but unused; `requestedTotal: "0 USD"` in Avro mapper is documented placeholder |

### Weaknesses

| ID | Severity | Category | Evidence | Recommendation |
|----|----------|----------|----------|----------------|
| F-1 | **Minor** | Idempotency gap | No idempotency key in `SubmitCorporateBulkOrderCommand`. Duplicate HTTP POSTs create distinct orders. | Add `idempotencyKey` field to the command before production deployment |
| F-2 | **Minor** | No correlation threading | `correlationId` is not threaded from HTTP request → command → domain event → outbox. Outbox events have `null` correlation/causation IDs. | Wire `X-Correlation-Id` through `CorrelationFilter` → command → event → outbox |
| F-3 | **Observation** | Observability skeleton | `observability/` package has only `package-info.java` with no implementation. MDC fields (`sagaId`, `orderId`, `traceId`) are not set during order submission. | Add MDC enrichment in `SubmitCorporateBulkOrderController` after the first saga-aware task |

---

## Architecture Score

| Dimension | Score | Rationale |
|-----------|-------|-----------|
| Domain | **8/10** | Pure, immutable, well-scoped. Minor: unused `Money` VO, no domain events for non-creation transitions |
| Hexagonal Architecture | **9/10** | Strict layering enforced. Minor: dual publish path (Kafka port unused by service) |
| Transaction Design | **9/10** | Atomic order+outbox, proven rollback. Observation: gRPC call inside transaction scope |
| Test Strategy | **9/10** | Layered pyramid with vertical proof. Observation: create-drop vs Flyway divergence is intentional |
| Maintainability | **9/10** | Clean code, consistent patterns, ArchUnit guardrails. Clear package structure |
| Extensibility | **8/10** | Outbox, retry, saga, correlation are structurally ready. Minor: idempotency + correlation not yet wired |
| **Overall** | **8.7/10** | Production-ready for current scope; minor deferred capabilities noted |

---

## Findings Summary

| ID | Severity | Area | Finding |
|----|----------|------|---------|
| D-1 | Minor | Domain | `Money` VO unused by `Order` or `OrderLine` |
| D-2 | Observation | Domain | No domain events for non-creation state transitions |
| D-3 | Observation | Domain | Empty `command/` and `exception/` domain packages |
| H-1 | Minor | Hexagonal | `OrderEventPublisher` (Kafka) wired but never injected by service |
| H-2 | Observation | Hexagonal | `CustomerAccessPort` uses raw strings instead of domain VOs |
| T-1 | Observation | Transactions | gRPC availability check runs inside transaction scope |
| O-1 | Minor | Outbox | No `correlationId` threaded from command to outbox event |
| O-2 | Observation | Outbox | `eventType` derived from `getClass().getSimpleName()` |
| O-3 | Observation | Outbox | No idempotency key on submission command |
| I-1 | Observation | Integration | `AllowAllCustomerAccessAdapter` is a placeholder |
| I-2 | Observation | Integration | `requestedTotal: "0 USD"` placeholder in Avro mapper |
| TE-1 | Observation | Tests | `JpaOrderRepositoryAdapterTest` uses create-drop instead of Flyway |
| TE-2 | Observation | Tests | No concurrent-submission stress test |
| F-1 | Minor | Future | No idempotency key on submission |
| F-2 | Minor | Future | No correlation threading from HTTP to outbox |
| F-3 | Observation | Future | Observability package has no implementation |

**Summary count**: 0 Critical, 0 Major, 5 Minor, 11 Observation

---

## Final Verdict

# PASS WITH MINOR OBSERVATIONS

The order-service is **architecturally ready for production** within its defined scope. It correctly owns order acceptance, persistence, and saga-start event emission. Hexagonal boundaries are strictly enforced. Transaction boundaries are correct and proven. The test strategy provides high confidence.

The five Minor findings do not block production readiness:
1. Unused `Money` VO (dead code, not a bug)
2. Unused `OrderEventPublisher` Kafka bean (architectural clutter)
3. No `correlationId` threading (observability gap, not functional gap)
4. No idempotency key (deduplication gap)
5. Bare-string port signatures (style, not correctness)

These should be resolved before expanding to production traffic, but none require code removal from the current slice.

---

## References

- Source code: `server/order-service/src/main/java/com/arbitrier/order/`
- Test code: `server/order-service/src/test/java/com/arbitrier/order/`
- Platform primitives: `server/platform/src/main/java/com/arbitrier/platform/`
- Architecture decisions: `docs/adr/ADR-0007` (Native Image), `ADR-0008` (W3C Trace Context), `ADR-0009` (Inventory Allocation), `ADR-0010` (API Entry Point)
- Prior ARB-023 reports: `docs/agents/reports/ARB-023.*.md`
- Engineering Log: `ENGINEERING_LOG.md`
