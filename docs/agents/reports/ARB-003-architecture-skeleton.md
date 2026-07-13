# ARB-003 — Architecture Skeleton Implementation Note

| Field  | Value      |
|--------|------------|
| Task   | ARB-003    |
| Status | Implemented |
| Date   | 2026-07-07 |

## What Was Created

### Maven Multi-Module Build

| File | Purpose |
|------|---------|
| `pom.xml` | Root parent POM — Spring Boot 4.1.0 parent, Java 25, ArchUnit dependency management |
| `server/pom.xml` | Server aggregator — declares build order |
| `server/platform/pom.xml` | Library jar; no Spring Boot plugin |
| `server/contracts/pom.xml` | Avro contracts jar; Avro Maven plugin commented, ready to activate |
| `server/order-service/pom.xml` | Spring Boot service; JPA + Kafka starters commented, ready to activate |
| `server/inventory-service/pom.xml` | Same pattern |
| `server/credit-service/pom.xml` | Same pattern |
| `server/orchestrator-service/pom.xml` | Same pattern |

### Java Sources

**Application classes (4):**
- `OrderServiceApplication`, `InventoryServiceApplication`, `CreditServiceApplication`, `OrchestratorServiceApplication`
- All are minimal `@SpringBootApplication` classes.
- Virtual threads enabled via `spring.threads.virtual.enabled=true` in `application.yml`.

**`package-info.java` files (56 total):**
- 14 per service × 4 services = 56
- 9 in platform
- 1 in contracts

All packages documented:

```
<service>/
  (root) · adapter/inbound/rest · adapter/inbound/kafka
  adapter/outbound/persistence · adapter/outbound/kafka
  application/port/inbound · application/port/outbound · application/service
  domain/model · domain/event · domain/command · domain/exception
  config · observability
```

### Test Classes

**Architecture tests (5):** One `ArchitectureTest` per service + `PlatformArchitectureTest`.
- Rules are written but commented out — activated when the first class is added to each layer.
- Three rules per service: domain→adapter, application→adapter, domain→Spring/JPA.

**Context load tests (4):** `*ApplicationIT.java` with `@SpringBootTest contextLoads()`.

**Contracts test (1):** `ContractsSchemaTest` placeholder for Avro schema compatibility.

### Resources

`application.yml` per service — app name, virtual threads enabled, Actuator health/info endpoints exposed.

## Dependency Decisions

| Dependency | Decision |
|-----------|----------|
| `spring-boot-starter-data-jpa` | Commented out — no entities yet |
| `postgresql` | Commented out — no datasource config yet |
| `spring-kafka` | Commented out — no Avro contracts yet |
| `avro-maven-plugin` | Commented out in contracts — activated in ARB-004+ |
| `archunit-junit5` | Active in all modules — rules are no-ops until classes exist |

## Naming Change from ARB-001

ARB-001 documentation used `in/out` and `jpa` for adapter sub-packages.  
ARB-003 uses `inbound/outbound` and `persistence` as defined in the task spec.  
`CLAUDE.md` has been updated to reflect the corrected naming.

## Deep Review Fixes (ARB-003-FIX-001)

| Change | Detail |
|--------|--------|
| `CONTRIBUTING.md` | Removed Pact reference; contract testing row now lists Avro Schema Registry only |
| `server/platform/pom.xml` | Removed `spring-boot-starter-security`; reintroduced in the Keycloak integration phase |

## Open Questions

- OPEN QUESTION: Exact Java 25 + Spring Boot 4.1.0 availability — build will pass once these versions are released.
- OPEN QUESTION: Maven wrapper (`.mvn/wrapper/`) — add if team standardizes on a specific Maven version.
