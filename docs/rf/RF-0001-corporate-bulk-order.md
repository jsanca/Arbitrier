# RF-0001 — Corporate Bulk Order

| Field | Value |
|-------|-------|
| Status | Draft |
| Date | 2026-07-07 |
| Canonical Detail | [RF-UC-01](RF-UC-01-corporate-bulk-order.md) |

## Intention

Provide the ARB-001 functional-requirement anchor for UC-01 and point implementation work to the expanded UC-01 requirement package.

## Context

ARB-002 expands the placeholder UC-01 documentation into [RF-UC-01](RF-UC-01-corporate-bulk-order.md), [UC-01](../okf/UC-01-corporate-bulk-order.md), [RNF-UC-01](../rnf/RNF-UC-01-saga-runtime.md), and detailed test cases.

## Decision or Requirement

Use [RF-UC-01](RF-UC-01-corporate-bulk-order.md) as the canonical functional requirement for UC-01.

Final states are:

- `CONFIRMED`
- `PARTIALLY_CONFIRMED`
- `CANCELLED`

The documented waiting state is:

- `AWAITING_CUSTOMER_DECISION`

## Inputs

See [RF-UC-01](RF-UC-01-corporate-bulk-order.md).

## Outputs

See [RF-UC-01](RF-UC-01-corporate-bulk-order.md).

## Preconditions

See [RF-UC-01](RF-UC-01-corporate-bulk-order.md).

## Postconditions

See [RF-UC-01](RF-UC-01-corporate-bulk-order.md).

## Failure Behavior

See [RF-UC-01](RF-UC-01-corporate-bulk-order.md).

## Observability Expectations

See [RF-UC-01](RF-UC-01-corporate-bulk-order.md).

## Test Evidence Placeholder

See [TC-UC-01](../test-cases/TC-UC-01-corporate-bulk-order.md) and the detailed TC-UC-01-001 through TC-UC-01-012 files.

## Open Questions

- OPEN QUESTION: Track unresolved UC-01 questions in [RF-UC-01](RF-UC-01-corporate-bulk-order.md) and [UC-01](../okf/UC-01-corporate-bulk-order.md).
