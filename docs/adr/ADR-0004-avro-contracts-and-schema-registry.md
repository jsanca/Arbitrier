# ADR-0004 — Avro Contracts and Schema Registry

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-07-07 |

## Context

RNF-0001 defines Kafka, Avro, and Confluent Schema Registry as the messaging baseline. UC-01 requires events such as `OrderCreated`, `StockReserved`, `StockPartiallyReserved`, `CreditApproved`, and `CreditRejected`.

## Decision

Kafka message contracts must be defined as Avro schemas under `server/contracts/` and validated through Schema Registry compatibility checks before production code consumes or produces them.

No Avro schemas are generated as part of ARB-002.

## Consequences

- Contract-first development is required for Kafka integration.
- Schema evolution must be explicit and reviewed.
- Services cannot introduce ad hoc JSON Kafka payloads for UC-01 without a new ADR.

## Open Questions

- OPEN QUESTION: Exact event and command schemas.
- OPEN QUESTION: Schema compatibility mode.
- OPEN QUESTION: Naming convention for command schemas if commands use Kafka.
