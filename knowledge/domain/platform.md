# Platform

## Purpose

Own domain-neutral shared capabilities used by the service contexts. Platform
enables consistent technical and application-level conventions without owning
corporate-order, inventory, credit, or saga business rules.

## Responsibilities

- Provide shared concepts for validation, time, error representation, and
  message and request identity.
- Provide shared conventions for correlation, tracing, safe logging, and
  observability.
- Provide domain-neutral foundations for idempotency and reliable message
  delivery.
- Preserve dependency direction from business contexts toward shared
  capabilities, never the reverse.

## Does NOT Own

- Orders, customers, products, stock, warehouses, credit, or sagas.
- Business aggregates, business persistence models, or business-state
  transitions.
- Business-specific contracts or a service context's external integrations.
- Architectural decisions that belong to the project governance process.

## Main Concepts

- **Shared concepts:** correlation, causation, request and message identity,
  validation, time, and problem semantics.
- **Domain services:** domain-neutral idempotency and message-delivery
  foundations.
- **Commands and events:** none as business concepts; Platform supports the
  conventions used by business contracts.
- **Operational concepts:** trace context, safe structured logging, and
  observability naming.

## Collaborates With

- **Order, Inventory, Credit, and Orchestrator:** each consumes Platform's
  domain-neutral capabilities; Platform does not depend on or coordinate them.
- **Contracts:** Platform supports shared delivery and observability conventions
  while Contracts owns wire-format definitions.

## Source of Truth

- [Server module guide](../../server/README.md)
- [Platform guide](../../server/platform/README.md)
- [Outbox, inbox, and idempotency ADR](../../docs/adr/ADR-0005-outbox-inbox-idempotency.md)
- [Trace-context ADR](../../docs/adr/ADR-0008-w3c-trace-context-propagation.md)
- [Technical baseline RNF](../../docs/rnf/RNF-0001-technical-baseline.md)

## Future Knowledge

- Shared reliability and idempotency concepts.
- Correlation and observability terminology.
- Message-delivery conventions.
- Platform operational knowledge.
