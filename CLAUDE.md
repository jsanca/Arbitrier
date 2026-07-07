# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Operating Instructions

Work documentation-first. Read the relevant OKF, RF, RNF, ADR, and test-case files before coding. Do not invent business behavior; mark missing details as `OPEN QUESTION`. Keep changes scoped to the requested slice.

---

## Build Commands

```bash
# Server — all modules
mvn -B verify --no-transfer-progress

# Server — single module
mvn -B verify --no-transfer-progress -pl server/order-service

# Server — single test class
mvn -B test -pl server/order-service -Dtest=PlaceBulkOrderServiceTest

# Server — single test method
mvn -B test -pl server/order-service -Dtest=PlaceBulkOrderServiceTest#methodName

# Client
cd client && npm ci && npm run build && npm test

# E2E (Playwright, against running stack)
cd client && npx playwright test
cd client && npx playwright test e2e/uc01-confirmed.spec.ts

# Local infrastructure (Kafka, PostgreSQL, Keycloak, Schema Registry)
docker compose -f infra/docker/docker-compose.yml up -d
docker compose -f infra/docker/docker-compose.yml down -v
```

---

## Repository Layout

```
server/
  order-service/        Order aggregate; saga entry point
  inventory-service/    Stock reservation and compensation
  credit-service/       B2B credit limit validation
  orchestrator-service/ Saga coordinator
  contracts/            Avro schemas + OpenAPI specs (shared)
  platform/             Library jar — security, observability, exceptions
client/                 React 19 / TypeScript portal
docs/
  okf/                  Use-case narratives (UC-NN) and project index
  rf/                   Functional requirements (RF-UC-NN)
  rnf/                  Non-functional requirements
  adr/                  Architecture decision records
  test-cases/           Test case specs (TC-UC-NN and TC-UC-NN-NNN)
infra/
  docker/               Local docker-compose + init-db.sql
  k8s/                  Kubernetes manifests
  terraform/            GCP infrastructure
  strimzi/              Kafka on Kubernetes
```

---

## Hexagonal Architecture (every server service)

```
com.arbitrier.<service>/
  adapter/
    inbound/
      rest/        HTTP-driven adapters (Spring MVC controllers)
      kafka/       Event-driven adapters (Kafka consumers)
    outbound/
      persistence/ JPA-driven adapters (Spring Data repositories)
      kafka/       Kafka producer adapters
  application/
    port/
      inbound/     Use-case interfaces called by inbound adapters
      outbound/    Repository and messaging interfaces implemented by adapters
    service/       Use-case implementations — depend only on ports and domain
  domain/
    model/         Entities and value objects — zero Spring/JPA imports
    event/         Domain events
    command/       Commands
    exception/     Domain exceptions
  config/          Spring @Configuration — wires adapters to ports
  observability/   MDC helpers (sagaId, orderId, traceId) and OpenTelemetry spans
```

Dependency rule: `adapter → application → domain`. Nothing flows outward.  
`domain` must have **zero Spring, JPA, or Kafka imports**.

### `package-info.java` — required in every package

```java
/**
 * <One-sentence purpose.>
 *
 * <p>Layer: [domain/model | domain/event | application/port/inbound | adapter/inbound/rest | ...]
 * <p>Module: <service-name>
 */
package com.arbitrier.<service>.<layer>;
```

---

## UC-01 Saga States

```
STARTED → CREDIT_RESERVATION_REQUESTED
  → CREDIT_RESERVED → INVENTORY_RESERVATION_REQUESTED
      → INVENTORY_FULLY_RESERVED                              → CONFIRMED
      → INVENTORY_PARTIALLY_RESERVED → AWAITING_CUSTOMER_DECISION
            → (customer accepts)                              → PARTIALLY_CONFIRMED
            → (customer rejects) [compensations run]         → CANCELLED
      → INVENTORY_RESERVATION_FAILED [compensate credit]     → CANCELLED
  → CREDIT_RESERVATION_DENIED                                 → CANCELLED
```

The waiting state is `AWAITING_CUSTOMER_DECISION` — not "human approver."

---

## Documentation Conventions

All RF, RNF, ADR, and TC documents use the OKF template in this section order:

```
## Intention
## Context
## Decision or Requirement
## Inputs
## Outputs
## Preconditions
## Postconditions
## Failure Behavior
## Observability Expectations
## Test Evidence Placeholder
## Open Questions
```

Use `OPEN QUESTION:` (all caps) for unresolved items. Never invent values for open questions.

### Document naming

| Kind | Pattern | Example |
|------|---------|---------|
| Use case narrative | `docs/okf/UC-<NN>-<slug>.md` | `UC-01-corporate-bulk-order.md` |
| Functional requirement | `docs/rf/RF-UC-<NN>-<slug>.md` | `RF-UC-01-corporate-bulk-order.md` |
| Non-functional requirement | `docs/rnf/RNF-<NNNN>-<slug>.md` or `RNF-UC-<NN>-<slug>.md` | `RNF-UC-01-saga-runtime.md` |
| Architecture decision | `docs/adr/ADR-<NNNN>-<slug>.md` | `ADR-0002-orchestrated-saga-kafka.md` |
| Test case index | `docs/test-cases/TC-UC-<NN>-<slug>.md` | `TC-UC-01-corporate-bulk-order.md` |
| Test case detail | `docs/test-cases/TC-UC-<NN>-<NNN>-<slug>.md` | `TC-UC-01-001-create-pending-order.md` |

`docs/rf/RF-0001-corporate-bulk-order.md` is a legacy redirect from ARB-001. The canonical UC-01 RF is `RF-UC-01-corporate-bulk-order.md`.

### Planned ADRs (not yet written)

ADR-0002 Orchestrated Saga with Kafka · ADR-0003 Schema per Service in PostgreSQL · ADR-0004 Avro Contracts and Schema Registry · ADR-0005 Outbox, Inbox, and Idempotency · ADR-0006 SSE vs WebSocket for Saga Dashboard

---

## Naming Conventions

| Artifact | Pattern |
|----------|---------|
| Input port | `<Action><Subject>UseCase` (interface) |
| Output port | `<Subject>Repository`, `<Subject>Gateway` |
| Application service | `<Action><Subject>Service` |
| REST adapter | `<Subject>RestAdapter` |
| JPA adapter | `<Subject>JpaAdapter` |
| Kafka consumer adapter | `<Subject>KafkaConsumerAdapter` |
| Kafka producer adapter | `<Subject>KafkaProducerAdapter` |
| Domain event | `<Subject><PastTense>Event` |
| Avro schema file | `<subject>-<past-tense>-event-v<N>.avsc` |
| Kafka topic | `<domain>.<event>.v<N>` (e.g. `order.placed.v1`) |

---

## Backend Style

- Java 25 · Spring Boot 4.1.0 · JPA for persistence adapters.
- `KafkaTemplate` for Kafka publishing when contracts exist.
- Resilience4j for retries, timeouts, and circuit breakers.
- SLF4J structured logs — every saga log line must include `sagaId`, `orderId`, `traceId`.
- OpenTelemetry for distributed traces.
- Every public API has Javadocs.

## Frontend Style

- React 19 · TypeScript strict mode.
- Dashboard for saga status, event timeline, and pending customer decision.
- Playwright E2E for UC-01 visible workflows.
- Do not assume SSE or WebSocket until ADR-0006 is resolved.

## Testing Expectations

- Unit tests for domain and application rules.
- Integration tests for adapters, persistence, Kafka, and compensation behavior.
- Contract tests for Avro schemas after contracts exist.
- Playwright E2E for buyer and operator workflows.
- Idempotency tests for duplicate events and commands.
- TC-UC-01-001 through TC-UC-01-012 map to the test index in `docs/test-cases/TC-UC-01-corporate-bulk-order.md`.

## Scope Guardrails

- No production code before docs and tests define the slice.
- No Kafka consumers or producers before Avro contracts exist.
- No generated Avro sources committed — generated at build time only.
- No hidden business logic in adapters.
- No new final UC-01 states without updating the docs first.
- No bypassing Kafka/Avro decisions without an ADR.
- `platform` must have no business domain knowledge (no order/credit/inventory types).
