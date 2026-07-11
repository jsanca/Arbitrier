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
| KF-001 | UC-01 Corporate Bulk Order saga      | Domain/application implemented; runtime messaging pending | [UC-01](UC-01-corporate-bulk-order.md), [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |
| KF-002 | B2B credit reservation               | Domain, application, and JPA implemented; external credit source pending | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |
| KF-003 | Inventory reservation + compensation | Domain, multi-warehouse allocation, application, and JPA implemented | [ADR-0009](../adr/ADR-0009—GlobalInventoryAllocationOwnership.md) |
| KF-004 | Partial-availability buyer decision  | Implemented before order/saga submission; no saga customer-wait state | [ARB-017](../implementation/ARB-017-pre-saga-availability-negotiation.md) |
| KF-005 | Saga state observability             | HTTP correlation foundation implemented; full runtime timeline pending | [RNF-0001](../rnf/RNF-0001-technical-baseline.md) |
| KF-006 | Keycloak-based B2B identity          | Order JWT integration and local realm implemented; production membership adapter pending | [ARB-010](../implementation/ARB-010-security-integration.md) |
| KF-007 | React Customer Portal                | Mock-backed prototype complete; backend integration pending | [ARB-UI-001](../implementation/ARB-UI-001-customer-portal-react-prototype.md) |
| KF-008 | Browser E2E coverage                 | Planned after backend integration | [TC-UC-01](../test-cases/TC-UC-01-corporate-bulk-order.md) |
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
| ADR-0008 | W3C Trace Context Propagation | Accepted |
| ADR-0009 | Global Inventory Allocation Ownership | Accepted |

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
