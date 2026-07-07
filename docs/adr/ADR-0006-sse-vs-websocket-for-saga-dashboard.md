# ADR-0006 — SSE vs WebSocket for Saga Dashboard

| Field | Value |
|-------|-------|
| Status | Proposed |
| Date | 2026-07-07 |

## Context

UC-01 requires a dashboard where buyer or operator can see order status, saga state, event timeline, and pending human decisions. The source material asks whether SSE or WebSocket should be used.

## Decision

OPEN QUESTION: Choose SSE or WebSocket after dashboard interaction requirements are documented.

Until then, implementations must not assume a live transport. They may document read-model requirements and ordinary status reads, but must not implement a live update mechanism.

## Consequences

- The dashboard requirement remains documented without inventing transport behavior.
- Frontend E2E tests can initially target visible status and timeline once APIs exist.
- A future ADR update must select and justify the live update transport.

## Open Questions

- OPEN QUESTION: Is one-way server-to-client status streaming enough?
- OPEN QUESTION: Are operators expected to send commands through the same live channel?
- OPEN QUESTION: Expected update frequency and connection scale.
