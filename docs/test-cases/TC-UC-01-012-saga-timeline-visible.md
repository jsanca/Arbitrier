# TC-UC-01-012 — Saga Timeline Is Visible

| Field | Value |
|-------|-------|
| Status | Draft |
| Type | E2E Playwright |
| Requirement | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |

## Intention

Verify that buyer or operator can inspect current saga status and event timeline.

## Context

This test covers UC-01.06 and supports the React dashboard requirement.

## Decision or Requirement

Given an order has emitted multiple saga events, when buyer or operator opens the dashboard, then the UI displays current status, event timeline, and pending action if the saga is waiting for customer decision.

## Inputs

- Existing order with saga events.
- Buyer or operator session.

## Outputs

- Current order status.
- Current saga state.
- Event timeline.
- Pending decision UI when state is `AWAITING_CUSTOMER_DECISION`.

## Preconditions

- Frontend can authenticate through Keycloak.
- Backend status API exists.
- Saga timeline data exists.

## Postconditions

- User can see final or waiting state without inspecting logs.

## Failure Behavior

- Dashboard behavior for missing timeline data is OPEN QUESTION.
- Authorization behavior for internal operator versus buyer is OPEN QUESTION.

## Observability Expectations

- Frontend status reads are traceable to backend timeline reads.
- UI test evidence includes screenshots or trace artifacts.

## Test Evidence Placeholder

- Playwright evidence pending implementation.

## Open Questions

- OPEN QUESTION: SSE or WebSocket for live saga status updates.
- OPEN QUESTION: Exact dashboard route.
- OPEN QUESTION: Exact role names for buyer and internal operator.
