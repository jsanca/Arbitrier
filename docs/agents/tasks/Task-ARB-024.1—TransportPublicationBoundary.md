Task: ARB-024.1 — Transport Publication Boundary

Status:
[DONE]

Owner:
Clio

Role:
Architecture Alignment / Focused Implementation

Timebox:
Target: 20–30 minutes
Hard stop: 45 minutes

Apply:

.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

Do not commit.

Context

ARB-023 established the production flow:

Order application service
↓
Order persistence
↓
Transactional Outbox
↓
future Outbox Relay
↓
Kafka
↓
Saga Orchestrator

Deep V4 Pro identified that the Order Service currently also contains an
OrderEventPublisher port and a KafkaOrderEventPublisher adapter that are not used
by the submission use case.

Before implementing Avro transport and Schema Registry integration, ARB-024.1
must make the publication boundary explicit and remove ambiguity between:

1. direct application-service publication to Kafka; and
2. publication from the transactional Outbox relay.

This task defines the single production publication path.

Do not implement Schema Registry.

Do not implement new Avro serialization.

Do not implement the Outbox relay unless a minimal existing component requires
adjustment to preserve the selected boundary.

----------------------------------------------------------------------
1. Establish the publication rule
----------------------------------------------------------------------

The intended production rule is:

Business application services do not publish directly to Kafka.

They persist domain events through the transactional Outbox.

Only the transport/outbox relay layer may publish Outbox events to Kafka.

Expected production path:

Application Service
↓
Domain Event
↓
DomainEventToOutboxMapper
↓
OutboxRepository
↓
commit
↓
Outbox Relay
↓
Transport Adapter
↓
Kafka

Explicitly reject this production path:

Application Service
↓
OrderEventPublisher
↓
Kafka

Document this rule in the appropriate architecture documentation or package
documentation.

----------------------------------------------------------------------
2. Inspect the current direct publisher path
----------------------------------------------------------------------

Review:

- OrderEventPublisher
- KafkaOrderEventPublisher
- OrderCreatedAvroMapper
- their Spring wiring
- tests related to direct publication
- any production references or injections

Determine whether these types:

A. are dead and should be removed now;
B. belong to the future Outbox Relay transport layer and should be relocated or
renamed;
C. are still needed as a generic transport abstraction but are currently placed
at the wrong boundary.

Do not preserve unused types merely because tests exist.

Do not remove a type if it already represents the correct future relay boundary;
instead clarify its ownership and naming.

----------------------------------------------------------------------
3. Define the transport port ownership
----------------------------------------------------------------------

If a Kafka publishing port remains, it must belong conceptually to the Outbox
Relay or transport layer, not to the Order application use case.

Prefer a boundary such as:

Outbox relay application
→ EventTransportPublisher port
→ Kafka/JSON/Avro transport adapter

Avoid an order-specific publication port if the future relay is intended to
publish events from multiple bounded contexts.

Evaluate whether:

OrderEventPublisher

should become something like:

OutboxEventPublisher
EventTransportPublisher
TransportPublisher

Do not rename broadly unless the current code makes the new ownership obvious.

The task should leave a coherent boundary, not necessarily complete the final
generic relay design.

----------------------------------------------------------------------
4. JSON development and Avro production strategy
----------------------------------------------------------------------

Document the intended adapter strategy for later ARB-024 slices:

Development/test transport adapter:

Outbox Relay
→ JSON transport adapter
→ Kafka or local test transport

Production transport adapter:

Outbox Relay
→ Avro transport adapter
→ Schema Registry
→ Kafka

JSON and Avro must be alternative adapters behind the same transport boundary.

They must not be parallel publications of the same event.

Do not implement these adapters in this task.

----------------------------------------------------------------------
5. Preserve the transactional rule
----------------------------------------------------------------------

Ensure no application service:

- invokes KafkaTemplate;
- injects a Kafka publisher;
- publishes before transaction commit;
- performs best-effort dual writes;
- publishes both to Outbox and Kafka.

The successful Order submission must remain:

save Order
save Outbox
commit

and nothing more.

----------------------------------------------------------------------
6. Stable event identity follow-up
----------------------------------------------------------------------

Deep observed that the Outbox event type currently derives from:

event.getClass().getSimpleName()

This is relevant to future transport, subject naming, and contract evolution.

Do not redesign event identity unless the change is small and clearly required
to establish the publication boundary.

At minimum, document a focused follow-up for ARB-024 to define:

- stable logical event type;
- event version;
- subject naming input;
- independence from Java class names.

Do not allow Schema Registry subjects to depend implicitly on Java class names.

----------------------------------------------------------------------
7. Tests
----------------------------------------------------------------------

Add or adjust focused tests proving:

1. the Order submission application service has no direct Kafka publisher
   dependency;
2. successful submission persists the Outbox event and does not publish directly;
3. removed or relocated publisher types have no remaining production references;
4. conditional Kafka wiring does not accidentally create a direct Order-service
   publication path;
5. existing ARB-023 behavior remains unchanged.

Prefer architecture tests or focused dependency tests where appropriate.

Do not add tests for Schema Registry or Avro compatibility yet.

----------------------------------------------------------------------
8. Architecture enforcement
----------------------------------------------------------------------

Where practical, add an ArchUnit rule preventing:

application.. packages

from depending on:

KafkaTemplate
Kafka producer adapters
transport publication adapters

The rule should permit:

application
→ OutboxRepository port

and forbid:

application
→ Kafka publication

Keep the rule general enough to protect future services without coupling it to
one class name if the current architecture-test structure supports that.

----------------------------------------------------------------------
9. Production changes
----------------------------------------------------------------------

Allowed:

- remove unused direct-publish port or adapter;
- relocate or rename a transport abstraction;
- adjust Spring configuration;
- remove obsolete tests;
- add architecture documentation;
- add ArchUnit protection;
- add focused regression tests.

Not allowed:

- Schema Registry integration;
- new Avro producer implementation;
- Kafka relay implementation beyond boundary clarification;
- changes to Order acceptance semantics;
- changes to Saga behavior;
- direct publication after commit;
- broad platform redesign.

----------------------------------------------------------------------
10. Validation
----------------------------------------------------------------------

Run:

mvn -B test --no-transfer-progress \
-pl server/contracts,server/platform,server/order-service

Include other modules only if relocation creates a legitimate dependency.

Report:

- types removed, retained, renamed, or relocated;
- final publication boundary;
- Spring wiring changes;
- architecture rules added;
- tests affected;
- deferred work for ARB-024.2+;
- total tests and build result.

----------------------------------------------------------------------
Deliverables

- one explicit production publication path;
- no direct Order application → Kafka path;
- coherent ownership of any remaining transport publisher port;
- documented JSON-development / Avro-production adapter strategy;
- architecture enforcement where practical;
- focused implementation report.

Do not commit.

If incomplete at the hard stop, create:

docs/agents/checkpoints/CHECKPOINT-ARB-024.1.md

and stop.