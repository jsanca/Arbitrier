# ADR-0005 — Outbox, Inbox, and Idempotency

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-07-07 |

## Context

UC-01 requires at-least-once Kafka delivery, idempotent command processing, idempotent event processing, and reliable publication after local state changes.

## Decision

Use outbox records for events that must be published after database commits. Use inbox or equivalent idempotency records for consumed messages and commands. Every UC-01 handler must tolerate duplicate delivery.

## Consequences

- Duplicate `OrderCreated` does not duplicate sagas or stock reservation.
- Duplicate `ReleaseStock` does not create negative inventory.
- Publication and consumption behavior can be audited.

## Open Questions

- OPEN QUESTION: Exact outbox and inbox table schemas.
- OPEN QUESTION: Cleanup and retention policy.
- OPEN QUESTION: Whether idempotency keys come from event IDs, command IDs, request headers, or all of them.
