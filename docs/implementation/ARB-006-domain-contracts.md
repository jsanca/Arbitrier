# ARB-006 — Domain Contracts

| Field  | Value       |
|--------|-------------|
| Task   | ARB-006     |
| Status | Implemented |
| Date   | 2026-07-07  |

## Schemas Created

### Common (`com.arbitrier.contracts.common`)

| Schema | Type | Purpose |
|--------|------|---------|
| `MessageMetadata` | record | Standard metadata fields on every event/command |
| `MoneyAmount` | record | String decimal + ISO-4217 currency |
| `OrderLineContract` | record | SKU + quantity pairing for orders and reservations |
| `CancellationReason` | enum | CUSTOMER_CANCELLED · CUSTOMER_DEFERRED · INSUFFICIENT_CREDIT · SYSTEM_TIMEOUT |

### Order (`com.arbitrier.contracts.order`)

| Schema | Kind |
|--------|------|
| `OrderCreated` | event |
| `OrderConfirmed` | event |
| `OrderPartiallyConfirmed` | event |
| `OrderCancelled` | event |

### Inventory (`com.arbitrier.contracts.inventory`)

| Schema | Kind |
|--------|------|
| `ReserveStockRequested` | command |
| `StockReserved` | event |
| `StockPartiallyReserved` | event |
| `StockRejected` | event |
| `ReleaseStockRequested` | command |
| `StockReleased` | event |

### Credit (`com.arbitrier.contracts.credit`)

| Schema | Kind |
|--------|------|
| `ReserveCreditRequested` | command |
| `CreditApproved` | event |
| `CreditRejected` | event |
| `ReleaseCreditRequested` | command |
| `CreditReleased` | event |

### Orchestrator (`com.arbitrier.contracts.orchestrator`)

| Schema | Kind |
|--------|------|
| `CustomerDecision` | enum |
| `CompensationAction` | enum |
| `CustomerDecisionRequested` | event |
| `CustomerDecisionSubmitted` | event |
| `SagaCompleted` | event |
| `SagaCancelled` | event |
| `SagaCompensationFailed` | event |

## Metadata Convention

Every event and command includes a `metadata` field of type `MessageMetadata`:

| Field | Type | Notes |
|-------|------|-------|
| `messageId` | string | UUID per message |
| `correlationId` | string | Traces the full business transaction |
| `causationId` | `["null","string"]` | ID of the causing message; null for root |
| `occurredAt` | string | ISO-8601 UTC timestamp |
| `schemaVersion` | string | e.g. `"v1"` |

## Naming Convention

| Pattern | Meaning |
|---------|---------|
| `<Subject>Created / Confirmed / Cancelled` | Domain event (past tense) |
| `Reserve<Subject>Requested / Release<Subject>Requested` | Command (present imperative) |
| `<Subject>Approved / Rejected / Released` | Outcome event from downstream service |

## What Was Intentionally Not Implemented

| Area | Reason |
|------|--------|
| Kafka topics | ADR-0004 requires topic names decided separately |
| Producer/consumer code | No Kafka runtime code until adapters are implemented |
| Service dependencies on generated classes | Each service will declare the dependency when its adapter is implemented |
| Confluent Schema Registry compatibility checks | Requires live Schema Registry; deferred to integration phase |
| `tenantId` field | Out of scope for v1 (single-tenant) |

## Native Image Considerations

- Generated Avro classes use reflection for serialization/deserialization. When service modules start using them, a `RuntimeHintsRegistrar` must register all generated classes.
- The Avro runtime library itself uses reflection internally. Compatibility with Native Image must be validated when the first Kafka adapter is implemented.
- The contracts module is a library jar and does not itself run as a native executable.

## Design Note — `OrderCreated.requestedTotal`

`OrderCreated` carries a `requestedTotal` field of type `MoneyAmount`. The source of truth for this value is not yet finalized. Candidate approaches:

| Candidate | Notes |
|-----------|-------|
| Application layer computes it from catalog/pricing data at order-submission time | Keeps Order domain free of pricing; requires catalog integration before order-service can emit `OrderCreated` |
| `order-service` stores it when pricing is modeled | Defers catalog integration; Order aggregate would need a `requestedTotal` field |
| Dedicated catalog/pricing service owns the value | Cleanest bounded-context separation; adds a synchronous or event-driven dependency before saga can start |

No decision is made in ARB-006. The field exists in the contract so downstream consumers (orchestrator, dashboard) can display the requested amount. The implementing task must resolve this before wiring the `OrderCreated` Kafka producer.

## Open Questions

- OPEN QUESTION: Source of truth for `OrderCreated.requestedTotal` — application layer, order-service pricing model, or catalog service (see Design Note above).
- OPEN QUESTION: Exact Kafka topic names for each event/command (deferred to ADR-0004 update).
- OPEN QUESTION: Schema compatibility mode (BACKWARD, FORWARD, FULL) for Schema Registry.
- OPEN QUESTION: Whether commands also travel over Kafka or only events (ADR open question from UC-01).
- OPEN QUESTION: Whether generated Avro classes need a `RuntimeHintsRegistrar` before or only when first consumed by a service.
- OPEN QUESTION: Should `MoneyAmount` carry a scale field for precision beyond 2 decimal places?
