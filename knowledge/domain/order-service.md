# Order

## Purpose

Own the corporate order lifecycle and the buyer-facing entry into a submitted
order. Order turns buyer-selected quantities into an order outcome; it does not
own the distributed work needed to establish that outcome.

## Responsibilities

- Prepare an advisory availability review before submission and accept the
  buyer's resulting choice.
- Create and maintain the order's business lifecycle and buyer-visible outcome.
- Initiate the post-submission collaboration that establishes stock and credit
  results.

## Does NOT Own

- Warehouse selection, stock allocation, or authoritative stock reservation.
- Credit-limit decisions or credit reservation.
- Cross-context saga progression, retry policy, or compensation coordination.
- Shared technical conventions and delivery infrastructure.

## Main Concepts

- **Aggregate:** Order.
- **Value objects:** customer identity, product line, quantity, monetary amount,
  and cancellation reason.
- **Domain services:** order submission and advisory availability preparation.
- **Commands:** prepare a prospective order, record a buyer pre-submission
  decision, and submit selected order lines.
- **Events:** order created, confirmed, partially confirmed, and cancelled.

## Collaborates With

- **Inventory:** Order requests an advisory, non-binding availability view before
  submission; Inventory returns business-level availability, not warehouse detail.
- **Orchestrator:** Order initiates post-submission coordination and receives the
  final order outcome.
- **Credit:** Order has no direct credit-decision responsibility; that
  collaboration is coordinated by Orchestrator.
- **Platform:** Order uses domain-neutral shared capabilities without extending
  their ownership into the order domain.

## Source of Truth

- [Corporate bulk-order requirement](../../docs/rf/RF-UC-01-corporate-bulk-order.md)
- [UC-01 narrative](../../docs/okf/UC-01-corporate-bulk-order.md)
- [Orchestrated saga ADR](../../docs/adr/ADR-0002-orchestrated-saga-with-kafka.md)
- [Global inventory allocation ADR](../../docs/adr/ADR-0009—GlobalInventoryAllocationOwnership.md)
- [Order service guide](../../server/order-service/README.md)
- [Contract module guide](../../server/contracts/README.md)

## Future Knowledge

- Order lifecycle and outcome reference.
- Order events and public contract reference.
- Pre-submission availability and buyer-decision knowledge.
- Order data and retention concepts.
