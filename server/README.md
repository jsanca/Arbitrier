# Server Modules

All backend services are Java 25 / Spring Boot 4.1.0 applications built in a Maven multi-module layout.

## Module Relationships

```
arbitrier (root pom)
└── server (aggregator pom)
    ├── platform          Library jar — security, observability, idempotency, logging, exceptions
    ├── contracts         Library jar — Avro schemas (.avsc) and generated event DTOs
    ├── order-service     ──┐
    ├── inventory-service   ├── Each depends on platform + contracts
    ├── credit-service      │   Spring Boot deployable apps
    └── orchestrator-service┘
```

`platform` and `contracts` must be built first (Maven respects module order in `server/pom.xml`).

## Hexagonal Package Structure

Every service follows the same layout:

```
com.arbitrier.<service>/
  adapter/
    inbound/
      rest/        HTTP-driven adapters (Spring MVC controllers)
      kafka/       Event-driven adapters (Kafka consumers)
    outbound/
      persistence/ Driven adapters (JPA repositories)
      kafka/       Driven adapters (Kafka producers)
  application/
    port/
      inbound/     Use-case interfaces called by inbound adapters
      outbound/    Repository and messaging interfaces implemented by outbound adapters
    service/       Use-case implementations — depend only on ports and domain
  domain/
    model/         Entities and value objects — zero Spring/JPA imports
    event/         Domain events
    command/       Commands
    exception/     Domain exceptions
  config/          Spring @Configuration
  observability/   MDC helpers and OpenTelemetry spans
```

## Build

```bash
# From repo root — build and test all server modules
mvn -B verify --no-transfer-progress

# Single module
mvn -B verify --no-transfer-progress -pl server/order-service

# Single test
mvn -B test -pl server/order-service -Dtest=ArchitectureTest
```

## Module Summaries

| Module | Artifact ID | Role |
|--------|-------------|------|
| `platform` | `arbitrier-platform` | Cross-cutting library; no business logic |
| `contracts` | `arbitrier-contracts` | Avro schemas and generated event types |
| `order-service` | `order-service` | Order aggregate; saga entry point |
| `inventory-service` | `inventory-service` | Stock reservation and compensation |
| `credit-service` | `credit-service` | B2B credit limit validation |
| `orchestrator-service` | `orchestrator-service` | Saga coordinator; UC-01 state machine |

## Status

`ARB-003` — Architecture skeleton complete. No business logic. No domain models. No JPA entities. No Kafka consumers/producers.
