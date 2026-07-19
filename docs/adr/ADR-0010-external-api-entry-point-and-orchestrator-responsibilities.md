# ADR-0010 — External API Entry Point and Orchestrator Responsibilities

| Field | Value |
|---|---|
| Status | Planned |
| Date | 2026-07-18 |

## Context

UC-01 starts when a corporate buyer submits selected order lines. Order Service owns the Order aggregate and accepts that business command. After the Order is accepted, a distributed workflow reserves Inventory, evaluates Credit, and reaches an Order confirmation or rejection outcome.

The orchestration layer needs to coordinate that workflow without becoming the public façade for all clients. Making the Orchestrator the primary REST entry point would give it responsibility for request shaping, client aggregation, Order ownership, and workflow coordination. Those concerns have different lifecycles and would weaken bounded-context ownership.

The current implementation names the Order handoff event `OrderCreated`. This ADR uses `OrderSubmitted` in the conceptual sequence below to describe the accepted-order handoff; reconciling the public event name is deferred and this ADR does not rename an implemented contract.

## Decision

Order Service owns the external command API for Order creation. The Orchestrator owns the distributed workflow only after Order Service has accepted the Order and published the accepted-order handoff event.

The Orchestrator is not an API Gateway, Backend for Frontend (BFF), or owner of Order resources. It does not expose the primary REST API for creating Orders.

### Canonical flow

```text
UI
 ↓ REST
Order Service
 ↓ persist Order
 ↓ publish OrderSubmitted
Orchestrator
 ↓
Inventory
 ↓
Credit
 ↓
Order confirmation / rejection
```

`OrderSubmitted` represents the business handoff after a successfully accepted Order. In the current implementation that handoff is represented by `OrderCreated`; the sequence intentionally does not prescribe a code or schema change.

## Responsibilities

### UI

The UI collects buyer intent, invokes the Order Service external API, displays the accepted Order response, and presents subsequent status supplied by an appropriate query API. It does not construct saga commands, select warehouses, coordinate Inventory and Credit, or aggregate service responses by calling internal services directly.

### Order Service

Order Service owns external Order-creation commands and Order resources. It authenticates and authorizes the caller at its API boundary, validates Order-owned input, persists the Order, and publishes the accepted-order handoff event. It returns an Order-owned response; it does not synchronously coordinate the Inventory/Credit workflow on behalf of the UI.

### Orchestrator

The Orchestrator consumes accepted-order events and owns saga state, workflow sequencing, compensation coordination, and idempotent reaction to workflow outcomes. It sends internal commands to Inventory and Credit and determines when the workflow has succeeded or must compensate.

It does not expose the primary REST API, own Order resources, act as an API Gateway, aggregate UI responses, or replace a BFF. It also does not accept client-supplied saga state as authoritative.

### Inventory Service

Inventory Service owns stock availability, reservation, release, warehouse selection, and allocation. It processes the Orchestrator's internal reservation or release requests and reports business outcomes. It does not expose warehouse allocation decisions through the Order creation command; ADR-0009 remains authoritative for that boundary.

### Credit Service

Credit Service owns credit validation, reservation or consumption, release where applicable, and credit-policy outcomes. It processes internal workflow requests and reports its result to the Orchestrator. It does not own Order resources or the cross-service workflow sequence.

## Explicit Non-Responsibilities

The Orchestrator must not:

* expose the primary REST API for Order creation;
* own or persist Order resources as its business resource;
* act as an API Gateway for public service routing;
* aggregate UI responses across Order, Inventory, Credit, and saga state;
* replace a BFF by tailoring API responses for a client application;
* let a client directly start, advance, retry, or compensate a saga.

## Alternatives Considered

### Option A — UI → Order Service → Saga Orchestrator

```text
UI
 ↓
Order Service
 ↓
Saga Orchestrator
```

**Decision: Accepted.** Order Service remains the external command boundary and publishes an event after accepting the Order. The Orchestrator then coordinates the asynchronous distributed workflow.

### Option B — UI → Saga Orchestrator → Order Service

```text
UI
 ↓
Saga Orchestrator
 ↓
Order Service
```

**Decision: Rejected.** This mixes orchestration with an API façade, increases coupling between client/API concerns and workflow state, and weakens Order Service ownership of Order resources and acceptance rules.

## Consequences

* The Order-creation API remains stable even as workflow sequencing changes.
* The UI receives an accepted Order response, not a synchronous promise that Inventory and Credit will complete successfully.
* The Orchestrator remains focused on reliable distributed workflow behavior and compensation.
* A future status query/read model is needed to provide UI visibility into asynchronous progress.

## Future Evolution

An API Gateway or BFF remains an explicit architectural option and is intentionally deferred. It becomes appropriate when one or more of these conditions make a distinct edge layer valuable:

* multiple public services need centralized routing, authentication integration, or rate limiting;
* multiple client applications need different API compositions or response shapes;
* UI aggregation requires data from several bounded contexts in a single client-oriented response;
* cross-cutting edge policies such as centralized routing, rate limiting, or API analytics cannot be cleanly owned by the individual public services.

Introducing either component requires a new ADR that defines its ownership, public contracts, security model, failure behavior, and relationship to service APIs. It does not change the Orchestrator into a Gateway or BFF.

## Deferred Concerns

* Public API versioning and idempotency-key conventions.
* The public status/timeline query API and its read model.
* The concrete `OrderSubmitted` event name and schema migration, if one is needed.
* Authorized operational intervention and replay procedures.

## References

* [ADR-0002 — Orchestrated Saga with Kafka](ADR-0002-orchestrated-saga-with-kafka.md)
* [ADR-0009 — Global Inventory Allocation Ownership](ADR-0009—GlobalInventoryAllocationOwnership.md)
* [RF-UC-01 — Corporate Bulk Order](../rf/RF-UC-01-corporate-bulk-order.md)
* [UC-01 — Corporate Bulk Order](../okf/UC-01-corporate-bulk-order.md)
