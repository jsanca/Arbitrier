# Arbitrier Project Blueprint

**Status:** Draft
**Version:** 0.1
**Author:** Jsanca
**Last updated:** 2026-07-06

---

# 1. Purpose

Arbitrier is a runtime for executing business Use Cases via
Orchestrated Sagas.

It is not intended to be merely an example of Kafka or microservices.

Its purpose is to demonstrate an architecture where the Use Case is the
primary citizen of the system and the infrastructure is a mechanism to
materialize said use case.

Implementation must be a consequence of domain design.

---

# 2. Philosophy

Before writing code we must understand the domain.

The project follows the idea that:

Understanding → Model → Design → Implementation → Verification

Not:

Implementation → Refactoring → Understanding.

Code must be a translation of the business model.

---

# 3. Objectives

The project will serve to research and demonstrate:

- Orchestrated Sagas
- Hexagonal Architecture
- Event Driven Architecture
- Domain Driven Design
- Distributed observability
- Microservices
- Modern CI/CD
- Cloud Native Infrastructure
- Use Case-based Architecture

---

# 4. Non-Functional Objectives

The project will use:

- Java 25
- Spring Boot 4.1.0
- PostgreSQL
- Separate schemas per microservice
- Kafka
- Avro
- Schema Registry
- Keycloak
- JPA
- Resilience4j
- SLF4J
- OpenTelemetry
- Docker
- Kubernetes
- Terraform
- Google Cloud
- GitHub Actions
- React
- Playwright

---

# 5. Architecture

```
                React

                  │

             REST API

                  │

         Use Case Layer

                  │

        Saga Orchestrator

                  │

       Commands / Events (Kafka)

                  │

      Inventory / Credit / Order
```

Each service has:

- Its own database (dedicated schema)
- Its own API
- Single responsibility
- Its own events

---

# 6. Architectural Principles

## Business First

Every technical decision must answer to the domain first.

---

## Use Cases First

Use Cases are the center of the project.

Not the Controllers.

Not the Endpoints.

Not Kafka.

---

## Explicit Orchestration

Business decisions live in the Orchestrator.

They are not implicitly distributed among microservices.

---

## Explicit Compensation

Every compensable operation must have a documented compensation.

---

## Idempotency

Every command must be executable multiple times.

Every event must be processable multiple times.

---

## Observability First

Every important transition must generate:

- Logs
- Trace
- Metrics

---

## Documentation First

Every feature must exist first as documentation.

FR

NFR

Use Case

Test Case

ADR

Then implementation.

---

# 7. Hexagonal Architecture

Each service will follow:

```
Controller

↓

Application

↓

Domain

↓

Ports

↓

Adapters
```

The domain will not know about:

Spring

Kafka

JPA

REST

Infrastructure

---

# 8. Testing

The Test Pyramid will be followed.

Unit Tests

↓

Integration Tests

↓

Contract Tests

↓

End to End (Playwright)

---

# 9. Observability

Every request must have:

CorrelationId

TraceId

SagaId

OrderId

When applicable.

Every layer must generate logs.

Controller

Application

Repository

Kafka

---

# 10. Security

Authentication:

Keycloak

Authorization:

Roles

JWT

OAuth2

---

# 11. Frontend

React

Saga Dashboard

Event timeline

Real-time status

The decision between SSE or WebSocket remains open.

---

# 12. AI Workflow

Clio

Responsible for the Backend.

Deep

Technical review.

Elito

Infrastructure.

Documentation.

Evaluation.

Brio

Frontend.

MiniMax

Auxiliary tasks.

No agent shall invent business rules.

If a rule does not exist it must be marked as:

OPEN QUESTION

---

# 13. Initial Use Cases

- [UC-01 Corporate Bulk Order with B2B Credit](seeds/UC-01-corporate-bulk-order.md)

New Use Cases will be added subsequently.

---

# 14. Incremental Implementation

All development will be done through small Slices.

Each Slice must be:

Compilable.

Testable.

Documented.

Observable.

---

# 15. Definition of Done

A task is considered complete only when it has:

- Code
- Tests
- Javadocs
- package-info.java
- Logs
- Observability
- OKF Documentation
- ADR (if applicable)
- Review by Deep

---

# 16. Responsibility of this Document

This document represents the architectural intent of the project.

All subsequent documentation (FR, NFR, UC, ADR, Test Cases) must derive from this Blueprint.

If there is a conflict between another document and this Blueprint, an ADR must be opened to resolve the difference.

The Blueprint is the primary source of architectural truth.

---

# 17. Discovery-Based Engineering

Arbitrier recognizes two distinct phases of development.

## Discovery

Objective:

Understand the problem.

Allowed:

- prototypes
- spikes
- mocks
- experimentation

Code generated during Discovery is not considered production.

---

## Engineering

Once the domain is understood:

- the Blueprint is written
- Use Cases are consolidated
- FR and NFR are documented
- ADRs are written
- implementation begins

Implementation must not discover the domain.

It must materialize it.
