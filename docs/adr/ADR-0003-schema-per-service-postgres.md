# ADR-0003 — Schema per Service in PostgreSQL

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-07-07 |

## Context

RNF-0001 requires PostgreSQL and separate schemas per microservice. UC-01 spans order, inventory, credit, and orchestrator data.

## Decision

Each backend service owns its own PostgreSQL schema. Services must not share tables or bypass application ports to read another service's persistence model.

The concrete schemas are `order_service`, `inventory_service`, `credit_service`, and `orchestrator_service`. The local stack may also create a reserved `platform` schema, but platform contains no business tables. Keycloak uses a separate `keycloak` database.

## Consequences

- Service ownership remains explicit.
- Cross-service state is exchanged through APIs and Kafka contracts, not shared tables.
- Local transactions stay inside one service boundary.

## Open Questions

- OPEN QUESTION: Whether saga timeline storage belongs to orchestrator-service only or is projected elsewhere for dashboard reads.
