# TC-UC-01 — Corporate Bulk Order Test Package

| Field | Value |
|-------|-------|
| Status | Active index — legacy scenarios labeled |
| Date | 2026-07-07 |
| Requirement | [RF-UC-01](../rf/RF-UC-01-corporate-bulk-order.md) |

## Intention

Provide the aggregate test-case index for UC-01.

## Context

ARB-002 expands the original placeholder into detailed, one-scenario-per-file test specifications. This file remains as the package index.

## Decision or Requirement

UC-01 test coverage must preserve the documented business guarantees, final states `CONFIRMED`, `PARTIALLY_CONFIRMED`, and `CANCELLED`, pre-saga partial-availability decisions, explicit compensation, and idempotency.

## Inputs

- UC-01 source documents.
- Detailed test cases TC-UC-01-001 through TC-UC-01-012.

## Outputs

- A traceable index from UC-01 scenarios to future automated tests.

## Preconditions

- UC-01, RF-UC-01, and RNF-UC-01 exist.

## Postconditions

- Implementers can locate the detailed test case for each UC-01 scenario.

## Failure Behavior

- If a scenario requires an undocumented rule, the detailed test case must mark it as `OPEN QUESTION`.

## Observability Expectations

- Test cases that touch saga transitions must assert or capture logs, traces, metrics, or dashboard visibility where implementation makes that possible.

## Test Evidence Map

| TC ID | Scenario | Type | Document |
|-------|----------|------|----------|
| TC-UC-01-001 | Create pending order | Integration | [TC-UC-01-001](TC-UC-01-001-create-pending-order.md) |
| TC-UC-01-002 | Happy path full confirmation | Integration / E2E | [TC-UC-01-002](TC-UC-01-002-happy-path-full-confirmation.md) |
| TC-UC-01-003 | Pre-saga partial availability and buyer decision | Unit / Integration | [TC-UC-01-003](TC-UC-01-003-partial-backorder-human-decision.md) |
| TC-UC-01-004 | Historical in-saga accept-partial target — superseded by TC-003 | Historical | [TC-UC-01-004](TC-UC-01-004-buyer-accepts-partial-shipment.md) |
| TC-UC-01-005 | Backorder deferral — deferred future requirement | Deferred | [TC-UC-01-005](TC-UC-01-005-buyer-waits-for-backorder.md) |
| TC-UC-01-006 | Historical in-saga cancellation — superseded by pre-saga cancel | Historical | [TC-UC-01-006](TC-UC-01-006-buyer-cancels-partial-order.md) |
| TC-UC-01-007 | Credit rejected compensation | Integration | [TC-UC-01-007](TC-UC-01-007-credit-rejected-compensation.md) |
| TC-UC-01-008 | Inventory timeout | Integration | [TC-UC-01-008](TC-UC-01-008-inventory-timeout.md) |
| TC-UC-01-009 | Credit timeout after stock reserved | Integration | [TC-UC-01-009](TC-UC-01-009-credit-timeout-after-stock-reserved.md) |
| TC-UC-01-010 | ReleaseStock is idempotent | Unit / Integration | [TC-UC-01-010](TC-UC-01-010-release-stock-idempotent.md) |
| TC-UC-01-011 | Duplicate OrderCreated is idempotent | Integration | [TC-UC-01-011](TC-UC-01-011-duplicate-order-created-idempotent.md) |
| TC-UC-01-012 | Saga timeline is visible | E2E Playwright | [TC-UC-01-012](TC-UC-01-012-saga-timeline-visible.md) |

## Open Questions

- OPEN QUESTION: Automated test class names and locations will be assigned during implementation.
