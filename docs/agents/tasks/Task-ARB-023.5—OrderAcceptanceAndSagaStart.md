Task: ARB-023.5 — Order Acceptance and Saga Start

Status:
[DONE]

Owner:
Clio

Role:
Implementation

Timebox:
Target: 20–30 minutes
Hard stop: 45 minutes

Apply:

.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

Do not commit.

Context

ARB-023.3 completed the corporate bulk order submission flow up to:

- authorization;
- SKU normalization;
- inventory availability verification;
- Order creation;
- Order persistence;
- OrderCreatedDomainEvent persistence in the transactional Outbox.

This slice must complete the acceptance semantics and establish the explicit start
of the distributed Saga.

The Order Service owns Order acceptance.

The Orchestrator owns the distributed workflow after the Order-created event is
published.

Do not move REST ownership to the Orchestrator.

Do not invoke the Orchestrator synchronously from the Order Service.

----------------------------------------------------------------------
1. Define the acceptance transition
----------------------------------------------------------------------

Review the current Order state model and establish the exact state reached when
the submission use case succeeds.

The successful flow must make the acceptance semantics explicit.

Expected conceptual flow:

request
↓
authorize customer
↓
normalize order lines
↓
verify inventory availability
↓
create and persist Order
↓
persist Saga-start event in Outbox
↓
return accepted Order result

Use the existing domain states and terminology where possible.

Do not introduce a new state merely to match the task title if the current
domain model already represents the accepted/pending state correctly.

If the domain currently creates the Order directly in PENDING and PENDING means
"accepted for processing", preserve that meaning and document it clearly.

----------------------------------------------------------------------
2. Saga start event
----------------------------------------------------------------------

The successful Order submission must persist exactly one event that starts the
Saga.

Prefer the existing OrderCreatedDomainEvent if it already represents:

"The order has been accepted and is ready for distributed processing."

Do not introduce a duplicate OrderAccepted event unless there is a genuine
semantic distinction in the current architecture.

Confirm that the event includes the information required by the Orchestrator,
including at minimum:

- orderId;
- customerId;
- submittedBy;
- normalized order lines.

The event must contain the same normalized quantities that were validated and
persisted.

----------------------------------------------------------------------
3. Transactional consistency
----------------------------------------------------------------------

Order persistence and Outbox persistence must remain atomic.

Within the same transaction:

- save the Order;
- save the Saga-start Outbox record.

A failure writing the Outbox record must roll back the Order save.

A failure writing the Order must prevent creation of the Outbox record.

Do not publish directly to Kafka in this slice.

Do not add after-commit or best-effort publication logic.

----------------------------------------------------------------------
4. Correlation and message metadata
----------------------------------------------------------------------

Review the current Outbox mapping and platform observability facilities.

Do not add correlationId to the domain model or to
SubmitCorporateBulkOrderCommand merely for logging.

If the platform already supports technical message metadata, ensure the Saga-start
Outbox entry preserves the existing correlation/trace metadata.

If that wiring does not yet exist and adding it would expand the slice, document
it as a focused follow-up rather than redesigning the command or domain event.

----------------------------------------------------------------------
5. Idempotency and duplicate events
----------------------------------------------------------------------

Within one successful execution, exactly one Saga-start Outbox record must be
created.

Do not add broad request-level idempotency unless an existing mechanism already
exists in this use case.

Add protection or focused tests against accidental duplicate event persistence
inside the execution path.

Do not redesign the Outbox subsystem.

----------------------------------------------------------------------
6. Result semantics
----------------------------------------------------------------------

The REST/application result must communicate acceptance for processing, not
completion of the Saga.

The response must not imply that:

- inventory has been reserved;
- credit has been approved;
- the distributed order workflow has completed.

Use the current result contract unless it is semantically incorrect.

The returned state should correspond exactly to the persisted Order state.

----------------------------------------------------------------------
7. Tests
----------------------------------------------------------------------

Add or refine focused tests proving:

1. successful submission persists the Order;
2. successful submission persists exactly one Saga-start Outbox record;
3. persisted event contains normalized SKU quantities;
4. returned status equals the persisted Order status;
5. Outbox failure rolls back Order persistence;
6. Order persistence failure prevents Outbox persistence;
7. insufficient inventory creates neither Order nor Outbox event;
8. access denial creates neither Order nor Outbox event.

Prefer focused application/integration tests over mocks that cannot demonstrate
transactional rollback.

Use the existing test infrastructure and repository adapters.

Do not repair unrelated test infrastructure unless it blocks this slice directly.

----------------------------------------------------------------------
8. Architecture boundaries
----------------------------------------------------------------------

Preserve:

REST
→ Order application use case
→ Order domain
→ OrderRepository
→ OutboxRepository

Later:

Outbox relay
→ Kafka
→ Orchestrator

The Order Service must not:

- call the Orchestrator directly;
- own Saga state;
- reserve credit;
- reserve stock;
- wait for Saga completion.

----------------------------------------------------------------------
Validation

Run:

1. focused Order Service tests;
2. transactional persistence tests;
3. relevant module test suite.

Preferred command:

mvn -B test --no-transfer-progress \
-pl server/contracts,server/platform,server/order-service

Include inventory-service only if required by an existing integration test.

Do not expand into ARB-023.6 vertical integration proof.

----------------------------------------------------------------------
Deliverables

- explicit Order acceptance semantics;
- exactly one Saga-start event persisted through Outbox;
- atomic Order + Outbox persistence;
- focused tests;
- implementation report explaining:
    - accepted Order state;
    - why the selected event starts the Saga;
    - transaction boundary;
    - result semantics;
    - any deferred correlation metadata work.

Do not modify unrelated services.

Do not redesign the Saga Orchestrator.

Do not commit.

If incomplete at the hard stop, create:

docs/agents/checkpoints/CHECKPOINT-ARB-023.5.md

and stop.