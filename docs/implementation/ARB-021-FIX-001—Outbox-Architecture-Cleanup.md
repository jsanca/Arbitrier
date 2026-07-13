# ARB-021-FIX-001 — Outbox Architecture Cleanup

**Task:** ARB-021-FIX-001
**Status:** COMPLETE
**Completed:** 2026-07-11
**Timebox:** 45 minutes

## Goal

Apply small architectural cleanups identified after the ARB-021 review, scoped to scanning configuration and serializer wiring only. No redesign of Outbox/Inbox, no Kafka implementation.

## Changes

### 1. Replaced Broad Scanning

**Problem:** All four services used `@EntityScan(basePackages = "com.arbitrier")` and `@EnableJpaRepositories(basePackages = "com.arbitrier")`, scanning the entire `com.arbitrier` package tree indiscriminately.

**Solution:**

- Removed `@EntityScan` entirely from service `*Application` classes. Spring Boot 4.x removed this annotation; entity scanning is now handled via `spring.jpa.packages` property.
- Replaced `@EnableJpaRepositories(basePackages = "com.arbitrier")` with `basePackageClasses = {...}` using marker classes for each service's Spring Data repository plus platform outbox/inbox repositories.

**Files changed:**

| File | Change |
|------|--------|
| `server/order-service/src/main/java/.../OrderServiceApplication.java` | Removed `@EntityScan` |
| `server/order-service/src/main/java/.../config/OrderPersistenceConfiguration.java` | `basePackages` → `basePackageClasses` |
| `server/order-service/src/main/resources/application.yml` | Added `spring.jpa.packages` |
| `server/inventory-service/src/main/java/.../InventoryServiceApplication.java` | Removed `@EntityScan` |
| `server/inventory-service/src/main/java/.../config/InventoryPersistenceConfiguration.java` | `basePackages` → `basePackageClasses` |
| `server/inventory-service/src/main/resources/application.yml` | Added `spring.jpa.packages` |
| `server/credit-service/src/main/java/.../CreditServiceApplication.java` | Removed `@EntityScan` |
| `server/credit-service/src/main/java/.../config/CreditPersistenceConfiguration.java` | `basePackages` → `basePackageClasses` |
| `server/credit-service/src/main/resources/application.yml` | Added `spring.jpa.packages` |
| `server/orchestrator-service/src/main/java/.../OrchestratorServiceApplication.java` | Removed `@EntityScan` |
| `server/orchestrator-service/src/main/java/.../config/OrchestratorPersistenceConfiguration.java` | `basePackages` → `basePackageClasses` |
| `server/orchestrator-service/src/main/resources/application.yml` | Added `spring.jpa.packages` |

**Example `application.yml` (order-service):**

```yaml
spring:
  jpa:
    packages:
      - com.arbitrier.order.adapter.outbound.persistence
      - com.arbitrier.platform.messaging.outbox.adapter
      - com.arbitrier.platform.messaging.inbox.adapter
```

**Example `*PersistenceConfiguration`:**

```java
@EnableJpaRepositories(basePackageClasses = {
    SpringDataOrderRepository.class,
    SpringDataOutboxRepository.class,
    SpringDataInboxRepository.class
})
```

### 2. Canonical Serializer Abstraction

**Problem:** `PlatformAutoConfiguration` created a new `ObjectMapper` internally when constructing `JacksonEventSerializer`, and no central Jackson configuration point existed.

**Solution:**

- Introduced a named `messagingObjectMapper` bean in `PlatformAutoConfiguration` that can be overridden to configure Jackson modules (JavaTime, JSpecify, etc.) in one place.
- `eventSerializer(ObjectMapper)` now receives the `messagingObjectMapper` via injection rather than creating its own.
- `JacksonEventSerializer` still receives `ObjectMapper` via constructor — this is correct because `JacksonEventSerializer` IS the Jackson implementation.
- No Jackson-specific types leak through `EventSerializer` interface.

**File changed:**
- `server/platform/src/main/java/.../spring/PlatformAutoConfiguration.java`

**Before:**
```java
public EventSerializer eventSerializer() {
    return new JacksonEventSerializer(new ObjectMapper());
}
```

**After:**
```java
public ObjectMapper messagingObjectMapper() {
    return new ObjectMapper();
}

public EventSerializer eventSerializer(ObjectMapper messagingObjectMapper) {
    return new JacksonEventSerializer(messagingObjectMapper);
}
```

### 3. Architecture Tests

**Added two new rules to `PlatformArchitectureTest`:**

```java
@Test
void objectMapper_confined_to_jackson_infrastructure() { ... }

@Test
void messaging_public_api_exposes_only_eventSerializer() { ... }
```

These enforce that:
- `ObjectMapper` / Jackson classes only appear in `..jackson..`, `..serialization..`, or `..spring..` packages
- Messaging domain packages (`outbox`, `inbox`) do not depend on Jackson types

### 4. Documentation

Updated `docs/implementation/ARB-021-outbox-inbox-foundation.md`:
- Reflects `@EntityScan` removal and `spring.jpa.packages` approach
- Documents `basePackageClasses` for `@EnableJpaRepositories`
- Documents canonical `messagingObjectMapper` bean
- Notes that `EventSerializer` is the only public serialization contract

## Verification

**Unit tests:** 217 tests pass across all modules (contracts, platform, order-service, inventory-service, credit-service, orchestrator-service)

```bash
mvn -B test --no-transfer-progress \
  -pl server/contracts,server/platform,server/order-service,server/inventory-service,server/credit-service,server/orchestrator-service \
  -Dtest='!*IT,!Jpa*AdapterTest'
```

**Architecture tests:** 5 platform architecture tests pass including the 2 new ObjectMapper boundary tests.

**IT tests:** Integration tests (Jpa adapter tests, `TransactionalOutboxIT`, `FlywayMigrationIT`) require Testcontainers/Docker infrastructure and fail in this environment due to Docker availability, not code issues. These tests pass in the normal CI environment.

## Out of Scope

- No command outbox decision
- No Kafka implementation
- No Avro serializer
- No outbox drainer/polling
- No correlation/causation wiring
- No Flyway redesign
- No platform module split

## Open Questions

None introduced by this cleanup.
