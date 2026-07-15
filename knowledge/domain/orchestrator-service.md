# Orchestrator

## Purpose

Own the cross-context saga that coordinates the submitted corporate order from
authoritative stock reservation through credit reservation, completion, or
compensation.

## Responsibilities

- Start and advance the corporate-order saga after an order is submitted.
- Coordinate stock and credit work in the business-required order.
- Decide saga progress, cancellation, compensation, and attempt-exhaustion
  handling.
- Request the resulting order outcome once the saga reaches a terminal result.

## Does NOT Own

- The buyer's pre-submission availability decision.
- The order aggregate or customer-facing order lifecycle.
- Stock allocation, warehouse selection, or stock-reservation invariants.
- Credit-limit evaluation or credit-reservation invariants.
- Shared platform capabilities or the meaning of another context's data.

## Main Concepts

- **Aggregate:** corporate bulk-order saga.
- **Value objects:** saga identity, saga status, step, retry decision, and
  compensation action.
- **Domain services:** saga transition and retry-decision policy.
- **Commands:** request stock reservation or release, request credit reservation
  or release, and request order confirmation or cancellation.
- **Events:** saga started, advanced, completed, cancelled, compensated, and
  compensation failed.

## Collaborates With

- **Order:** Orchestrator is initiated by a submitted order and returns the
  coordinated final outcome to Order.
- **Inventory:** Orchestrator requests authoritative stock work and consumes the
  resulting reservation outcomes.
- **Credit:** Orchestrator requests credit work after the applicable stock
  outcome and consumes the resulting credit outcomes.
- **Platform:** Orchestrator uses domain-neutral shared capabilities while
  retaining all business coordination decisions.

## Source of Truth

- [Corporate bulk-order requirement](../../docs/rf/RF-UC-01-corporate-bulk-order.md)
- [UC-01 narrative](../../docs/okf/UC-01-corporate-bulk-order.md)
- [Orchestrated saga ADR](../../docs/adr/ADR-0002-orchestrated-saga-with-kafka.md)
- [Outbox, inbox, and idempotency ADR](../../docs/adr/ADR-0005-outbox-inbox-idempotency.md)
- [Orchestrator service guide](../../server/orchestrator-service/README.md)
- [Contract module guide](../../server/contracts/README.md)

## Future Knowledge

- Saga lifecycle and compensation knowledge.
- Saga events and command-contract reference.
- Retry-policy knowledge.
- Saga data and observability concepts.
