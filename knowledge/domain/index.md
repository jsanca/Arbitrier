# Domain Knowledge

## Purpose

Navigate Arbitrier's bounded contexts, durable business concepts, invariants,
and workflows without replacing functional requirements or executable domain
behavior.

## Scope

Future pages may describe bounded contexts, aggregates, value objects,
business invariants, and domain workflows. They must remain conceptual and
link to their functional and implementation authority.

## Authoritative Sources

- [OKF index](../../docs/okf/index.md)
- [UC-01 narrative](../../docs/okf/UC-01-corporate-bulk-order.md)
- [Corporate bulk-order requirement](../../docs/rf/RF-UC-01-corporate-bulk-order.md)
- [Global inventory allocation ADR](../../docs/adr/ADR-0009—GlobalInventoryAllocationOwnership.md)

## Navigation

### Bounded Contexts

- [Order](order-service.md) — corporate order lifecycle and buyer-facing entry.
- [Inventory](inventory-service.md) — global availability, stock reservation,
  and internal allocation.
- [Credit](credit-service.md) — B2B credit reservation and release.
- [Orchestrator](orchestrator-service.md) — cross-context saga coordination and
  compensation.
- [Platform](platform.md) — domain-neutral shared capabilities.

These pages are a cartography layer, not replacements for requirements,
contracts, decisions, or service documentation. Add aggregate, value-object,
event, workflow, and data pages only when an authoritative source supports a
durable definition.
