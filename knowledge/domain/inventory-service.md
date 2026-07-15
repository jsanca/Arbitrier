# Inventory

## Purpose

Own global stock availability, authoritative stock reservation, release, and
warehouse allocation. Inventory exposes business-level quantities while keeping
logistics decisions inside its boundary.

## Responsibilities

- Provide advisory availability information for prospective order lines.
- Decide and record authoritative stock-reservation outcomes.
- Allocate requested quantities across warehouses according to Inventory policy.
- Release held stock when compensation requires it.

## Does NOT Own

- Buyer availability decisions or the order lifecycle.
- Cross-context saga sequencing, retry decisions, or compensation coordination.
- Credit-limit evaluation or credit reservation.
- Public warehouse selection or warehouse-topology disclosure.

## Main Concepts

- **Aggregate:** stock reservation.
- **Value objects:** reservation identity, requested stock line, allocation, and
  reservation outcome.
- **Domain services:** availability assessment and allocation/reservation
  decision-making.
- **Commands:** check availability, reserve stock, and release stock.
- **Events:** stock reserved, partially reserved, rejected, and released.

## Collaborates With

- **Order:** Inventory returns advisory availability for requested quantities;
  Order does not select warehouses.
- **Orchestrator:** Inventory receives authoritative reservation or release
  requests and returns reservation outcomes for saga coordination.
- **Credit:** Inventory has no direct credit collaboration.
- **Platform:** Inventory uses domain-neutral shared capabilities without
  delegating stock ownership to Platform.

## Source of Truth

- [Corporate bulk-order requirement](../../docs/rf/RF-UC-01-corporate-bulk-order.md)
- [Global inventory allocation ADR](../../docs/adr/ADR-0009—GlobalInventoryAllocationOwnership.md)
- [Schema-per-service ADR](../../docs/adr/ADR-0003-schema-per-service-postgres.md)
- [Inventory service guide](../../server/inventory-service/README.md)
- [Contract module guide](../../server/contracts/README.md)

## Future Knowledge

- Availability and reservation concepts.
- Stock events and contract reference.
- Allocation-policy and inventory-data knowledge.
- Inventory compensation knowledge.
