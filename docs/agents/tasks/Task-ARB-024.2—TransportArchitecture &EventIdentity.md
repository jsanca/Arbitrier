Task: ARB-024.2 — Transport Architecture & Event Identity

Status:
[DONE]

Owner:
Clio

Role:
Architecture Alignment / Focused Implementation

Timebox:
Target: 30–45 minutes
Hard stop: 60 minutes

Apply:

.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

Do not commit.

----------------------------------------------------------------------
Context
----------------------------------------------------------------------

ARB-024.1 established the publication boundary:

Application
↓
Transactional Outbox
↓
Outbox Relay
↓
Transport Layer
↓
Kafka

The transport layer will support multiple encodings without changing the relay
or application services.

The current package:

adapter/outbound/kafka

is too transport-specific.

ARB-024.2 establishes the transport architecture and the logical identity of
published events before implementing Avro or Schema Registry.

Do not implement Schema Registry.

Do not implement Kafka publishing.

Do not implement relay polling.

This task is architectural.

----------------------------------------------------------------------
1. Create the transport package structure
----------------------------------------------------------------------

Refactor the outbound adapter structure to express transport concerns instead of
Kafka concerns.

Target layout:

adapter

└── outbound

      └── transport

            ├── avro

            ├── json

            ├── kafka

            ├── model

            └── package-info.java

Guidelines:

avro/
Avro-specific mapping and serialization.

json/
JSON transport mapping.

kafka/
Kafka transport adapter(s).

model/
Shared transport abstractions.

Do not introduce unnecessary abstraction layers.

----------------------------------------------------------------------
2. Move existing transport code
----------------------------------------------------------------------

Relocate existing transport components into their proper packages.

Expected examples:

OrderCreatedAvroMapper
→ transport/avro

future Kafka publisher
→ transport/kafka

future JSON mapper
→ transport/json

Only relocate.

Do not redesign behavior.

----------------------------------------------------------------------
3. Introduce Event Identity
----------------------------------------------------------------------

Current implementation derives:

event.getClass().getSimpleName()

This is unsuitable for transport contracts.

Design a stable logical identity.

Recommended model:

EventDescriptor

containing:

- logical event type
- event version

Example:

OrderCreatedDomainEvent

↓

EventDescriptor

↓

type:
order.created

version:
1

The descriptor must be:

- stable
- independent of Java class names
- independent of Avro
- independent of Kafka
- independent of Schema Registry

Avoid leaking transport concerns into the domain.

----------------------------------------------------------------------
4. DomainEvent contract
----------------------------------------------------------------------

Evaluate the smallest clean change.

Possible approaches:

A)

DomainEvent

↓

descriptor()

B)

Annotation-based metadata

C)

Registry mapping

Choose the simplest architecture that:

- keeps the domain clean
- avoids reflection-heavy infrastructure
- supports future event evolution

Document the trade-offs.

----------------------------------------------------------------------
5. Transport metadata model
----------------------------------------------------------------------

Introduce transport metadata abstractions if appropriate.

Possible example:

TransportEventMetadata

- eventType
- eventVersion

Do not include:

- Kafka topics
- Schema Registry subjects
- partitions
- offsets

Those belong to infrastructure.

----------------------------------------------------------------------
6. Subject strategy preparation
----------------------------------------------------------------------

Prepare for future Schema Registry work.

The transport layer must be capable of deriving:

logical event

↓

transport identity

↓

subject

without depending on Java class names.

Do not implement the subject naming strategy yet.

Only establish the required inputs.

----------------------------------------------------------------------
7. Architecture documentation
----------------------------------------------------------------------

Update package documentation describing:

Application
↓
Outbox
↓
Relay
↓
Transport
├── JSON
├── Avro
└── Kafka

Clearly define responsibilities.

----------------------------------------------------------------------
8. Tests
----------------------------------------------------------------------

Update unit tests impacted by relocation.

Add tests proving:

- stable EventDescriptor values
- transport identity independent of Java class names

Do not add Kafka integration tests.

Do not add Schema Registry tests.

----------------------------------------------------------------------
9. Validation
----------------------------------------------------------------------

Run:

mvn -B test --no-transfer-progress \
-pl server/contracts,server/platform,server/order-service

Report:

- packages moved
- EventDescriptor design
- transport responsibilities
- future extension points
- build result

Do not commit.

If incomplete at the hard stop:

docs/agents/checkpoints/CHECKPOINT-ARB-024.2.md