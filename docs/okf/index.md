# Arbitrier — OKF Index

> **OKF** (Objectives and Key Features) is the top-level navigation document for all project knowledge.  
> Start here to understand scope, then follow links into RF, RNF, ADR, and test cases.

---

## Project Objective

Build a production-grade B2B saga orchestration platform that makes corporate bulk-order fulfillment explicit, compensatable, and auditable — from credit validation through inventory reservation to final order confirmation.

---

## Key Features

| ID     | Feature                              | Status      | Reference                                          |
|--------|--------------------------------------|-------------|----------------------------------------------------|
| KF-001 | UC-01 Corporate Bulk Order saga      | In Progress — happy path + compensation done (ARB-016); pre-saga availability negotiation done (ARB-017) | [UC-01](UC-01-corporate-bulk-order.md), [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |
| KF-002 | B2B credit limit validation          | In Progress | RF-0001 §3                                         |
| KF-003 | Inventory reservation + compensation | In Progress | RF-0001 §4                                         |
| KF-004 | Human decision gate (partial backorder) | In Progress — pre-saga availability negotiation implemented (ARB-017); human decision happens before saga start; AWAITING_CUSTOMER_DECISION saga state deferred | RF-0001 §5                                         |
| KF-005 | Saga state observability             | Planned     | [RNF-0001](../rnf/RNF-0001-technical-baseline.md)  |
| KF-006 | Keycloak-based B2B identity          | Planned     | RNF-0001 §5                                        |
| KF-007 | React corporate portal               | Planned     | —                                                  |
| KF-008 | Playwright E2E coverage              | Planned     | [TC-UC-01](../test-cases/TC-UC-01-corporate-bulk-order.md) |
| KF-009 | Reproducible local runtime infrastructure | Complete — ARB-027 | [Implementation](../implementation/ARB-027-local-runtime-stack.md) |

---

## Use Cases

| ID     | Name                       | Final States                                              | Document                                         |
|--------|----------------------------|-----------------------------------------------------------|--------------------------------------------------|
| UC-01  | Corporate Bulk Order       | `CONFIRMED` · `PARTIALLY_CONFIRMED` · `CANCELLED`         | [UC-01](UC-01-corporate-bulk-order.md), [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |

---

## Architecture Decisions

| ADR    | Title                    | Status   |
|--------|--------------------------|----------|
| ADR-0001 | Project Structure      | Accepted |
| ADR-0002 | Orchestrated Saga with Kafka | Accepted |
| ADR-0003 | Schema per Service in PostgreSQL | Accepted |
| ADR-0004 | Avro Contracts and Schema Registry | Accepted |
| ADR-0005 | Outbox, Inbox, and Idempotency | Accepted |
| ADR-0006 | SSE vs WebSocket for Saga Dashboard | Proposed |
| ADR-0007 | Spring AOT / GraalVM Native Image | Accepted |

See [`docs/adr/`](../adr/) for the full list.

---

## Non-Functional Requirements

| RNF    | Title                    | Status   |
|--------|--------------------------|----------|
| RNF-0001 | Technical Baseline     | Accepted |
| RNF-0002 | Native Image Runtime   | Draft    |
| RNF-UC-01 | Saga Runtime          | Draft    |

See [`docs/rnf/`](../rnf/) for the full list.

---

## Test Coverage Map

| UC     | Test Case Document                                                                 |
|--------|------------------------------------------------------------------------------------|
| UC-01  | [TC-UC-01](../test-cases/TC-UC-01-corporate-bulk-order.md), detailed TC-UC-01-001 through TC-UC-01-012 |

---

## Document Conventions

| Prefix | Meaning                        | Location            |
|--------|--------------------------------|---------------------|
| RF     | Requisito Funcional            | `docs/rf/`          |
| RNF    | Requisito Não-Funcional        | `docs/rnf/`         |
| ADR    | Architecture Decision Record   | `docs/adr/`         |
| TC     | Test Case                      | `docs/test-cases/`  |
| KF     | Key Feature (this document)    | `docs/okf/`         |
