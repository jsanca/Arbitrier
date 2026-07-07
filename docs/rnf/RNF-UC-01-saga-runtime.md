# RNF-UC-01 — Saga Runtime

| Field | Value |
|-------|-------|
| Status | Draft |
| Date | 2026-07-07 |
| Related Use Case | [UC-01](../okf/UC-01-corporate-bulk-order.md) |

## Intention

Define runtime constraints for the UC-01 orchestrated saga so implementation preserves consistency, idempotency, compensation, and observability.

## Context

UC-01 depends on Java 25, Spring Boot 4.1.0, PostgreSQL schemas per service, Kafka, Avro, Schema Registry, Resilience4j, SLF4J, OpenTelemetry, React, and Playwright as documented in RNF-0001.

## Decision or Requirement

- The saga must be explicitly orchestrated.
- Kafka and Avro contract decisions must not be bypassed without a new ADR.
- Each service must own its schema and must not share tables.
- Every event handler and command handler must be idempotent.
- Every compensable step must have an explicit compensation.
- Important state transitions must produce logs, metrics, and traces.
- Human waiting in `AWAITING_CUSTOMER_DECISION` is a business waiting state, not a downstream technical timeout.

## Inputs

- Saga commands and events for UC-01.
- Service-specific persistence records.
- Correlation identifiers: `sagaId`, `orderId`, and `traceId`.
- Resilience4j timeout and retry configuration.

## Outputs

- Durable saga state.
- Durable order state.
- Durable idempotency records or inbox/outbox records.
- Structured logs, traces, and metrics.
- Dashboard-readable saga timeline.

## Preconditions

- PostgreSQL, Kafka, Schema Registry, and Keycloak are available in local or deployed infrastructure.
- Avro contracts exist before implementation consumes or produces Kafka messages.
- Services follow hexagonal architecture boundaries.

## Postconditions

- Duplicate delivery does not corrupt state.
- Failed downstream steps leave the saga in a documented terminal state.
- Compensation can be retried without creating negative inventory or duplicate effects.
- Saga status can be inspected by buyer or operator.

## Failure Behavior

- At-least-once Kafka delivery requires idempotent consumers.
- Producer failure must not lose already-committed business state; use outbox where events depend on database commits.
- Consumer failure must not reapply already-handled messages; use inbox or equivalent idempotency records.
- Exhausted inventory or credit retries must transition to `CANCELLED` with `system_timeout`.
- Compensation failure handling policy is OPEN QUESTION.

## Observability Expectations

- Every log line produced during a saga step includes `sagaId`, `orderId`, and `traceId`.
- Metrics include saga starts, completions by final state, waiting-state entries, timeout cancellations, compensation attempts, compensation failures, and duplicate suppressions.
- Traces span REST entry points, application services, persistence, Kafka publish/consume, and dashboard reads.

## Test Evidence Placeholder

- Unit tests for state transition and idempotency rules: pending.
- Integration tests for outbox/inbox behavior: pending.
- Contract tests for Avro compatibility: pending.
- E2E dashboard evidence: pending.

## Open Questions

- OPEN QUESTION: Exact timeout SLA for inventory and credit.
- OPEN QUESTION: Exact retry count and backoff policy.
- OPEN QUESTION: Exact Kafka topic names and partitioning keys.
- OPEN QUESTION: Exact outbox and inbox table shape.
- OPEN QUESTION: Compensation failure escalation path.
- OPEN QUESTION: Retention period for saga timeline data.
