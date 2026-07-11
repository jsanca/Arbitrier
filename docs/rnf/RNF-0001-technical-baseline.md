# RNF-0001 — Technical Baseline

| Field    | Value                        |
|----------|------------------------------|
| Status   | Accepted                     |
| Date     | 2026-07-06                   |
| Version  | 1.0                          |

---

## Intention

Define the technical baseline that all Arbitrier implementation work must follow.

## Context

Arbitrier demonstrates use-case-driven, orchestrated saga implementation with Java, Spring Boot, PostgreSQL, Kafka, Avro, Schema Registry, Keycloak, React, and cloud-native infrastructure.

## Decision or Requirement

Use the baseline technologies and quality gates documented in the sections below unless a later ADR changes them.

## Inputs

- Project blueprint.
- UC-01 documentation package.
- Architecture decisions.

## Outputs

- Technical constraints for backend, frontend, infrastructure, testing, security, and observability.

## Preconditions

- Implementation work must read this baseline before selecting frameworks, libraries, messaging formats, or infrastructure patterns.

## Postconditions

- Services and clients use the documented baseline consistently.
- Deviations are documented through ADRs.

## Failure Behavior

- If a required baseline detail is missing, mark it as `OPEN QUESTION` and do not invent implementation behavior.
- If implementation requires a different technology choice, create or update an ADR before coding.

## Observability Expectations

- Logs, metrics, traces, and health signals follow the observability baseline in this document.
- Saga-related log lines include `sagaId`, `orderId`, and `traceId`.

## Test Evidence

- Maven unit, architecture, controller, contract-generation, and PostgreSQL Testcontainers coverage exists.
- Customer Portal unit/component tests exist; browser E2E and CI evidence remain pending.

## Open Questions

- OPEN QUESTION: Google Cloud region for deployed infrastructure.
- OPEN QUESTION: Exact UC-01 runtime timeout SLA and backoff; attempt-count decisions are implemented.
- OPEN QUESTION: Final Kafka topic/partition map; 26 Avro schemas are implemented.

## 1. Runtime Platform

| Requirement | Value       | Rationale                                                   |
|-------------|-------------|-------------------------------------------------------------|
| Language    | Java 25     | LTS release; virtual threads (Project Loom) for reactive-style blocking I/O without reactive frameworks |
| Framework   | Spring Boot 4.1.0 | Stable baseline for Java 25; native AOT support       |
| Build tool  | Maven 3.9+  | Reproducible builds; standard enterprise toolchain          |

All services must compile and pass tests on **Java 25** only. No multi-release jars.

---

## 2. Data

| Requirement          | Value              | Notes                                   |
|----------------------|--------------------|-----------------------------------------|
| Primary database     | PostgreSQL 16+     | Per-service schema; no shared tables    |
| ORM                  | JPA / Hibernate 7  | Managed via Spring Data JPA             |
| Schema migrations    | Flyway             | Versioned migration scripts per service |
| Schema isolation     | One schema per service | Enforced by separate datasource configs |

---

## 3. Messaging

| Requirement        | Value                   | Notes                                              |
|--------------------|-------------------------|----------------------------------------------------|
| Broker             | Apache Kafka 3.7+       | Managed via Strimzi on Kubernetes                  |
| Schema format      | Avro                    | Schemas in `server/contracts/`                     |
| Schema registry    | Confluent Schema Registry | Enforces schema evolution compatibility          |
| Consumer groups    | One per service per topic | Prevents cross-service consumer interference     |
| Delivery guarantee | At-least-once           | Idempotent consumers required at every handler     |

---

## 4. Resilience

| Requirement        | Library       | Patterns Applied                                    |
|--------------------|---------------|-----------------------------------------------------|
| Circuit breaker    | Resilience4j  | All synchronous inter-service HTTP calls            |
| Retry              | Resilience4j  | Kafka producers and idempotent HTTP calls           |
| Rate limiter       | Resilience4j  | External credit bureau calls                        |
| Timeout            | Resilience4j  | All outbound HTTP calls must declare a timeout      |
| Saga compensation  | Custom        | Every saga step must have a registered compensating transaction |

---

## 5. Identity and Security

| Requirement        | Value                                               |
|--------------------|-----------------------------------------------------|
| Identity provider  | Keycloak 25+                                        |
| Protocol           | OpenID Connect / OAuth 2.0                          |
| Token type         | JWT (Bearer)                                        |
| Service-to-service | Client credentials grant                           |
| User-facing API    | Authorization code + PKCE (React client)           |
| Local realm roles  | `CUSTOMER_USER`, `CUSTOMER_ADMIN`, `OPERATIONS_USER`, `OPERATIONS_ADMIN` |

---

## 6. Observability

| Signal   | Technology                          | Notes                                            |
|----------|-------------------------------------|--------------------------------------------------|
| Traces   | OpenTelemetry SDK + OTLP exporter   | W3C Trace Context (`traceparent`/`tracestate`) propagation (ADR-0008); no B3 unless legacy interop required |
| Metrics  | Micrometer → Prometheus             | Exposed on `/actuator/prometheus`                |
| Logs     | SLF4J → Logback                     | Structured JSON in production; trace ID injected |
| Health   | Spring Boot Actuator                | `/actuator/health` used by Kubernetes liveness   |

Correlation rule: every log line produced during a saga step **must** include `sagaId`, `orderId`, and `traceId`.

---

## 7. Frontend

| Requirement  | Value         | Notes                                         |
|--------------|---------------|-----------------------------------------------|
| Framework    | React 19+     | TypeScript strict mode                        |
| Build tool   | Vite          | Fast dev server; ESM output                   |
| E2E testing  | Playwright    | Test cases linked to UC-XX documents          |
| Auth         | OIDC via Keycloak JS adapter | Authorization code + PKCE flow   |

---

## 8. Infrastructure

| Layer          | Technology              | Notes                                          |
|----------------|-------------------------|------------------------------------------------|
| Containerization | Docker                | Multi-stage Dockerfiles; distroless base image |
| Orchestration  | Kubernetes              | GKE (Google Kubernetes Engine)                 |
| Kafka on K8s   | Strimzi Operator        | Manifests in `infra/strimzi/`                  |
| IaC            | Terraform               | GCP resources: GKE, Cloud SQL, Artifact Registry |
| CI/CD          | GitHub Actions          | Workflows in `.github/workflows/`              |
| Cloud          | Google Cloud Platform   | Region: to be decided in infra task            |

---

## 9. Testing Standards

| Level       | Minimum Coverage | Tooling                                    |
|-------------|------------------|--------------------------------------------|
| Unit        | 80% line coverage on `domain` and `application` | JUnit 5, Mockito |
| Integration | All adapters covered | Spring Boot Test, Testcontainers       |
| Contract    | All Avro schemas validated | Schema registry compatibility checks |
| E2E         | UC-01 happy path + main alternates | Playwright              |

---

## 10. Compliance and Quality Gates

- All PRs must pass CI (build + tests + lint) before merge.
- No `@SuppressWarnings("unchecked")` without an explanatory comment.
- Dependency updates via Dependabot; patch updates auto-merged on green CI.
- OWASP Dependency Check run weekly; CRITICAL CVEs block deployment.
