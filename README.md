# Arbitrier

**Arbitrier** is a B2B saga orchestration platform built on Java 25 and Spring Boot 4.1.0.  
It models the full lifecycle of a Corporate Bulk Order — from credit validation and inventory reservation through human decision gates for partial backorders to a final confirmed, partially confirmed, or cancelled outcome.

---

## Vision

Enterprise purchasing is a multi-party, multi-step process.  
Arbitrier makes those steps explicit, observable, and compensatable:

- Every saga step is a named transition with a clear owner (service or human).
- Every failure has a matching compensation that leaves the system in a consistent state.
- Every decision point — including the partial backorder human gate — is auditable.

---

## Domain: UC-01 Corporate Bulk Order

| Final State             | Condition                                          |
|-------------------------|----------------------------------------------------|
| `CONFIRMED`             | Credit approved + all items reserved               |
| `PARTIALLY_CONFIRMED`   | Credit approved + partial reservation, human okays |
| `CANCELLED`             | Credit denied or human rejects partial backorder   |

See [`docs/rf/RF-0001-corporate-bulk-order.md`](docs/rf/RF-0001-corporate-bulk-order.md) for the full functional requirement.

---

## Repository Layout

```
arbitrier/
├── server/
│   ├── order-service/          # Order lifecycle; saga entry point
│   ├── inventory-service/      # Inventory reservation and compensation
│   ├── credit-service/         # B2B credit limit validation
│   ├── orchestrator-service/   # Saga orchestrator (Spring State Machine / BPMN)
│   ├── contracts/              # Shared Avro schemas and API contracts
│   └── platform/               # Cross-cutting: security, observability, exceptions
├── client/                     # React frontend
├── docs/
│   ├── okf/                    # Project objectives and key features index
│   ├── adr/                    # Architecture Decision Records
│   ├── rf/                     # Requisitos Funcionais (Functional Requirements)
│   ├── rnf/                    # Requisitos Não-Funcionais (Non-Functional Requirements)
│   ├── test-cases/             # Test case specifications
│   └── diagrams/               # Architecture and sequence diagrams
├── infra/
│   ├── docker/                 # docker-compose for local dev
│   ├── k8s/                    # Kubernetes manifests
│   ├── terraform/              # Google Cloud IaC
│   └── strimzi/                # Kafka on Kubernetes via Strimzi
└── .github/
    └── workflows/              # GitHub Actions CI/CD
```

---

## Technical Baseline

| Layer            | Technology                          |
|------------------|-------------------------------------|
| Language         | Java 25                             |
| Framework        | Spring Boot 4.1.0                   |
| Database         | PostgreSQL                          |
| Messaging        | Apache Kafka + Avro (Schema Registry) |
| Identity         | Keycloak (OIDC / OAuth 2.0)         |
| Resilience       | Resilience4j                        |
| Observability    | OpenTelemetry + SLF4J               |
| Persistence      | JPA (Hibernate)                     |
| Frontend         | React (TypeScript)                  |
| E2E Testing      | Playwright                          |
| Container        | Docker / Kubernetes                 |
| Cloud            | Google Cloud Platform               |
| IaC              | Terraform                           |
| CI/CD            | GitHub Actions                      |

See [`docs/rnf/RNF-0001-technical-baseline.md`](docs/rnf/RNF-0001-technical-baseline.md) for full non-functional requirements.

---

## Architecture

All server-side modules follow **Hexagonal Architecture** (Ports & Adapters):

```
com.arbitrier.<service>/
├── domain/           # Entities, value objects, domain events — no framework deps
├── application/
│   ├── port/
│   │   ├── in/       # Use-case input ports (interfaces driven by adapters)
│   │   └── out/      # Output ports (repository, messaging interfaces)
│   └── service/      # Use-case implementations
├── adapter/
│   ├── in/           # Driving adapters: REST controllers, Kafka consumers
│   └── out/          # Driven adapters: JPA repositories, Kafka producers
└── config/           # Spring @Configuration classes
```

### `package-info.java` Policy

Every package **must** have a `package-info.java`.  
The file must contain at minimum:

```java
/**
 * <One-sentence purpose of this package.>
 *
 * <p>Layer: [domain | application | adapter | config]
 * <p>Module: <service-name>
 */
package com.arbitrier.<service>.<layer>;
```

This ensures Javadoc coverage and makes package intent explicit for new contributors.  
See [`CONTRIBUTING.md`](CONTRIBUTING.md) for the full convention guide.

---

## Documentation Index

| Folder              | Contents                                    |
|---------------------|---------------------------------------------|
| `docs/okf/`         | Objectives and Key Features index           |
| `docs/adr/`         | Architecture Decision Records               |
| `docs/rf/`          | Functional Requirements (RF-XXXX)           |
| `docs/rnf/`         | Non-Functional Requirements (RNF-XXXX)      |
| `docs/test-cases/`  | Test case specs linked to use cases         |
| `docs/diagrams/`    | C4, sequence, and state-machine diagrams    |

Start at [`docs/okf/index.md`](docs/okf/index.md).

---

## Getting Started (Local Dev)

> **Prerequisites:** Docker Desktop, Java 25 SDK, Node 20+

```bash
# Start infrastructure (Kafka, PostgreSQL, Keycloak, Schema Registry)
docker compose -f infra/docker/docker-compose.yml up -d

# (services and client build instructions will be added per module)
```

---

## Status

`ARB-001` — Repository layout complete. Business logic not yet implemented.
