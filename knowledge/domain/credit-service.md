# Credit

## Purpose

Own B2B credit-limit evaluation and the lifecycle of credit reservations used
to support a corporate order.

## Responsibilities

- Evaluate whether requested order value can be reserved against available B2B
  credit.
- Record approved or rejected credit-reservation outcomes.
- Release approved credit when compensation requires it.
- Preserve idempotent release behavior for credit that is no longer held.

## Does NOT Own

- The customer-facing order lifecycle or final order outcome.
- Stock availability, warehouse allocation, or stock compensation.
- Cross-context saga sequencing and the decision to begin compensation.
- The implementation of an external credit source or its business policy beyond
  the credit decision boundary.

## Main Concepts

- **Aggregate:** credit reservation.
- **Value objects:** reservation identity, customer identity, money amount, and
  reservation outcome.
- **Domain services:** credit availability evaluation and reservation release.
- **Commands:** reserve credit and release credit.
- **Events:** credit approved, rejected, and released.

## Collaborates With

- **Orchestrator:** Credit receives reservation or release requests and returns
  credit outcomes for coordinated progress or compensation.
- **Order:** Credit does not receive buyer-facing order submissions directly;
  it contributes a credit outcome to the order's eventual result.
- **Inventory:** Credit has no direct inventory-allocation responsibility.
- **Platform:** Credit uses domain-neutral shared capabilities without making
  them credit-domain policy.

## Source of Truth

- [Corporate bulk-order requirement](../../docs/rf/RF-UC-01-corporate-bulk-order.md)
- [Orchestrated saga ADR](../../docs/adr/ADR-0002-orchestrated-saga-with-kafka.md)
- [Outbox, inbox, and idempotency ADR](../../docs/adr/ADR-0005-outbox-inbox-idempotency.md)
- [Credit service guide](../../server/credit-service/README.md)
- [Contract module guide](../../server/contracts/README.md)

## Future Knowledge

- Credit reservation lifecycle reference.
- Credit events and contract reference.
- Credit data and external-credit integration concepts.
- Credit compensation knowledge.
