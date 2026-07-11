# ADR-0004 — Avro Contracts and Schema Registry

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-07-07 |

## Context

RNF-0001 defines Kafka, Avro, and Confluent Schema Registry as the messaging baseline. UC-01 requires events such as `OrderCreated`, `StockReserved`, `StockPartiallyReserved`, `CreditApproved`, and `CreditRejected`.

## Decision

Kafka message contracts must be defined as Avro schemas under `server/contracts/` and validated through Schema Registry compatibility checks before production code consumes or produces them.

ARB-006 delivered 26 Avro schemas covering UC-01 events and commands across four bounded contexts plus common shared types. Generated Java classes are produced at build time by `avro-maven-plugin` inside `contracts`. Service modules depend on contracts; order-service already maps `OrderCreated` for its conditional publisher foundation, while the remaining runtime adapters are pending.

## Consequences

- Contract-first development is required for Kafka integration.
- Schema evolution must be explicit and reviewed.
- Services cannot introduce ad hoc JSON Kafka payloads for UC-01 without a new ADR.
- All 26 schemas are documented in `docs/implementation/ARB-006-domain-contracts.md`.

## Open Questions

- Local development uses `BACKWARD` compatibility and `TopicNameStrategy`; production policy must confirm or supersede this before deployment.
- OPEN QUESTION: Naming convention for command schemas if commands use Kafka (see also UC-01 open question on whether commands travel over Kafka or only events).
- OPEN QUESTION: Exact Kafka topic names for each event and command.
