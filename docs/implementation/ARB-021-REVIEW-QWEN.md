# ARB-021 — Architectural Review

**Date:** 2026-07-11  
**Reviewer:** Qwen  
**Status:** COMPLETE

## Verdict

**PASS WITH WARNINGS**

ARB-021 provides a solid architectural foundation for Kafka integration. The outbox/inbox pattern is correctly implemented with proper transactional guarantees, clean separation of concerns, and appropriate platform ownership. Three warnings require attention before or during Kafka integration slices.

---

## Review Areas

### 1. Outbox Ownership

**Assessment: CORRECT**

Platform ownership of outbox/inbox infrastructure is appropriate:

- **Domain model** (`OutboxEvent`, `InboxEvent`) — platform-neutral value carriers
- **Ports** (`OutboxRepository`, `InboxRepository`) — infrastructure abstractions
- **JPA adapters** (`JpaOutboxRepositoryAdapter`, `JpaInboxRepositoryAdapter`) — technical implementation
- **Serializer** (`JacksonEventSerializer`) — cross-cutting concern
- **Mapper** (`DomainEventToOutboxMapper`) — transforms domain events to infrastructure records

**Rationale:** Outbox/inbox are infrastructure patterns, not business logic. Individual services should not reimplement serialization, entity mapping, or repository adapters. Platform ownership ensures consistency across all four services and prevents divergence.

**Architecture test enforcement:** `PlatformArchitectureTest` correctly enforces that messaging domain packages have zero Spring/JPA dependencies and adapter packages have zero business-domain dependencies.

---

### 2. Command vs Event Persistence

**Assessment: CORRECT with future consideration**

Current design:
- **Domain events** → persisted through `OutboxRepository` (transactional)
- **Saga commands** → use dedicated `*CommandPublisher` ports (direct Kafka, not yet implemented)

**Rationale:** This separation is architecturally sound:

- **Events** represent facts that occurred. They must be persisted atomically with the aggregate to guarantee at-least-once delivery. The outbox pattern solves the dual-write problem.
- **Commands** represent requests to other services. They are idempotent by design (saga commands carry saga IDs). If a command fails to publish, the saga can retry or timeout. Commands do not require transactional persistence because they are not state changes.

**Future consideration:** If command delivery guarantees become critical (e.g., exactly-once command processing), a command-outbox pattern could be introduced. However, this is not required for UC-01 and would add unnecessary complexity now. The current design correctly prioritizes simplicity.

**Recommendation:** No action required. Document this decision in ADR-0005 if not already present.

---

### 3. ObjectMapper Strategy

**Assessment: CORRECT**

Current implementation:
```java
@Bean
@ConditionalOnMissingBean
public EventSerializer eventSerializer() {
    return new JacksonEventSerializer(new ObjectMapper());
}
```

**Rationale:** Creating a dedicated `ObjectMapper` for the event serializer is correct:

- **Isolation:** Messaging serialization should not be affected by service-specific Jackson configuration (e.g., REST API date formats, null handling).
- **Consistency:** All outbox payloads use the same serialization rules regardless of which service writes them.
- **Versioning:** If serialization format changes (e.g., JavaTime module, custom deserializers), the messaging `ObjectMapper` can be configured independently without breaking REST APIs.

**Future evolution:** When Avro serialization is introduced (ARB-011 open question), the `EventSerializer` interface can be reimplemented with `KafkaAvroSerializer` without changing the outbox pipeline. The current design supports this evolution.

---

### 4. Entity Scanning

**Assessment: WARNING — Broad scanning scope**

Current implementation:
```java
@EntityScan(basePackages = "com.arbitrier")
@EnableJpaRepositories(basePackages = "com.arbitrier")
```

**Issue:** Every service scans the entire `com.arbitrier` package tree, including platform entities (`OutboxEventEntity`, `InboxEventEntity`) and all service-specific entities.

**Risk:** As the codebase grows, broad scanning may:
- Load unintended entities from test utilities or future modules
- Make it harder to reason about which entities belong to which service
- Increase startup time (minor, but measurable with many entities)

**Alternative:** Marker-class scanning would restrict entity discovery to specific packages:
```java
@EntityScan(basePackageClasses = {OrderEntity.class, OutboxEventEntity.class})
```

**Recommendation:**
- **Severity:** Low
- **Roadmap blocker?** No
- **Implementation now?** No
- **Future slice?** Consider during a refactoring slice if entity count exceeds ~20 or if modular boundaries become unclear.

**Justification:** The current approach works correctly and is simpler. The risk is theoretical at this scale. Marker-class scanning adds complexity without immediate benefit. Revisit if the codebase grows significantly or if multi-tenancy/modular deployment becomes a requirement.

---

### 5. Platform Modularity

**Assessment: ACCEPTABLE**

Platform module structure (16 packages):
```
correlation/      error/          exception/      idempotency/
kafka/            logging/        messaging/      observability/
result/           security/       spring/         test/
time/             validation/     web/
```

**Analysis:** Platform has accumulated responsibility for:
- Cross-cutting concerns (validation, error handling, time, correlation)
- Infrastructure abstractions (messaging, idempotency, kafka)
- Spring integration (auto-configuration, web filters)
- Testing utilities (in-memory adapters)

**Is this excessive?** No. Platform is a **library module**, not a service. It provides shared primitives that all services depend on. The messaging package is a natural fit because:
- Outbox/inbox are infrastructure patterns, not business logic
- Serialization is a cross-cutting concern
- Repository ports are infrastructure abstractions

**Risk:** Platform could become a "garbage collector" for unrelated concerns. However, the architecture test enforces that platform has zero business-domain dependencies, which prevents domain logic leakage.

**Recommendation:** No action required. Continue enforcing the "no business domain in platform" rule via architecture tests.

---

### 6. Flyway Strategy

**Assessment: CORRECT**

Current implementation:
- Platform contributes `V2__create_outbox_inbox_tables.sql`
- Each service includes `classpath:db/migration/platform` in Flyway locations
- Flyway executes the migration in the service's `default-schema` (e.g., `order_service`, `inventory_service`)

**Rationale:** This approach is superior to duplicated migrations:

- **Single source of truth:** Schema evolution happens in one place (platform module)
- **Consistency:** All services receive the same schema changes simultaneously
- **Versioning:** Platform migration version numbers are coordinated across services
- **Schema isolation:** Each service's outbox/inbox tables live in its own schema (no cross-service access)

**Future evolution risk:** If outbox/inbox schemas diverge per service (e.g., service-specific indexes, custom columns), the shared migration becomes a constraint. However, this is unlikely because:
- Outbox/inbox are infrastructure tables with uniform access patterns
- Service-specific extensions can be added via service-owned migrations (e.g., `V3__add_order_outbox_index.sql`)

**Recommendation:** No action required. The current design is correct and maintainable.

---

### 7. Transactional Guarantees

**Assessment: CORRECT**

Current flow:
```java
@Transactional
public Result execute(Command command) {
    Order order = Order.create(...);
    orderRepository.save(order);                    // JPA persist
    
    OrderCreatedDomainEvent event = new OrderCreatedDomainEvent(...);
    outboxRepository.save(outboxMapper.map(event, orderId, "Order"));  // JPA persist
    
    return new Result(orderId, order.status());
}
```

**Guarantees:**
- **Atomicity:** Aggregate and outbox event are persisted in the same transaction. If either fails, both roll back.
- **Consistency:** The outbox event is visible only after the aggregate is committed. No "orphan" events.
- **Isolation:** Concurrent transactions cannot see partial writes (PostgreSQL MVCC).
- **Durability:** After commit, the outbox row is durable even if the application crashes before Kafka publication.

**Dual-write risk:** None. The outbox pattern eliminates the dual-write problem by design. The only "dual write" is aggregate + outbox, which are in the same transaction.

**Integration test verification:** `TransactionalOutboxIT` proves that aggregate and outbox event are persisted atomically against a real PostgreSQL instance with Flyway migrations.

---

### 8. Future Kafka Integration

**Assessment: WELL-PREPARED**

Current design supports the following Kafka integration slices without major refactoring:

**Outbox Publisher (drainer):**
- `OutboxPublisher` interface exists as a placeholder
- `OutboxRepository.findPending()` provides the query interface
- `markPublished()` / `markFailed()` provide state transitions
- The drainer can be implemented as a separate service or scheduled task

**Kafka Producer:**
- `KafkaOrderEventPublisher` already exists (unused, but demonstrates the pattern)
- The producer can read pending outbox rows, deserialize payloads, and publish to Kafka
- Schema Registry integration (Avro) can be added without changing the outbox pipeline

**Inbox Consumer:**
- `InboxRepository` provides save/find/markProcessed operations
- `InboxEvent` record supports deduplication via `eventId` + `consumerId`
- The consumer can check inbox before processing to achieve idempotency

**Retry / DLQ:**
- `OutboxEvent.attemptCount` and `lastAttempt` fields support retry logic
- `publishStatus = FAILED` enables dead-letter identification
- The drainer can implement exponential backoff using these fields

**Refactoring required:** None. The current design is extensible.

---

## Warnings

### Warning 1: Entity Scanning Scope

**Category:** Modularity  
**Severity:** Low  
**Recommendation:** Consider marker-class scanning if entity count exceeds ~20 or modular boundaries become unclear.  
**Roadmap blocker?** No  
**Implementation now?** No  
**Future slice?** Refactoring (if needed)

---

### Warning 2: Correlation/Causation IDs Not Wired

**Category:** Observability  
**Severity:** Medium  
**Recommendation:** Wire correlation and causation IDs from REST/Kafka inbound adapters to outbox mapper during Kafka integration slice.  
**Roadmap blocker?** No (deferred to Kafka slice)  
**Implementation now?** No  
**Future slice?** Kafka integration (natural fit)

**Rationale:** The outbox schema includes `correlation_id` and `causation_id` columns, but they are currently written as `null`. This is acceptable for ARB-021 (foundation slice), but must be resolved before production to enable end-to-end traceability.

---

### Warning 3: JSON Serialization Placeholder

**Category:** Contract Evolution  
**Severity:** Medium  
**Recommendation:** Plan Avro serialization migration when Schema Registry integration is implemented (ARB-011 open question).  
**Roadmap blocker?** No (deferred to contracts-first slice)  
**Implementation now?** No  
**Future slice?** Schema Registry integration

**Rationale:** The current JSON serialization is a placeholder. Avro serialization will be required for production to ensure schema evolution and compatibility. The `EventSerializer` interface supports this evolution, but the migration will require:
- Avro schema registration for all domain events
- Serializer implementation using `KafkaAvroSerializer`
- Payload format migration strategy (JSON → Avro)

---

## Conclusion

**Is ARB-021 a solid architectural foundation for the next Kafka integration slices?**

**Yes.** The outbox/inbox pattern is correctly implemented with:
- Proper transactional guarantees (no dual-write risk)
- Clean separation of concerns (platform owns infrastructure, services own business logic)
- Extensibility (supports Kafka producer, consumer, retry, DLQ without refactoring)
- Test coverage (unit, integration, architecture tests)

The three warnings are low-to-medium severity and can be addressed during Kafka integration slices. No architectural changes are required.

**Next steps:**
1. Proceed with Kafka outbox drainer implementation
2. Wire correlation/causation IDs during drainer slice
3. Plan Avro serialization migration as a separate slice
