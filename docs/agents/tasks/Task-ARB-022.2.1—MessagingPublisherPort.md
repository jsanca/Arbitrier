Task: ARB-022.2.1 — Messaging Publisher Port

Status:
[PLANNED]

Owner:
Clio

Role:
Implementation

Timebox:
Target: 20–30 minutes

Hard stop:
45 minutes

Apply:

.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

If the hard stop is reached:

Create:

docs/agents/checkpoints/CHECKPOINT-ARB-022.2.1.md

and stop.

--------------------------------------------------

Context

ARB-021 established the durable Outbox / Inbox persistence model.

ARB-022.1 introduced the outbound message model, MessageNature, and routing
foundation.

The next roadmap step is Kafka integration.

Before introducing Kafka-specific adapters, the Platform layer should expose a
runtime-neutral publishing abstraction.

This slice introduces only the abstraction.

No Kafka classes should appear.

--------------------------------------------------

Goal

Introduce the runtime-neutral outbound messaging port that Application Services
will eventually use instead of publishing directly through Kafka-specific APIs.

This slice defines the contract only.

No runtime implementation is required.

--------------------------------------------------

Requirements

Create the publishing abstraction in Platform.

The interface should clearly express the intention of publishing an outbound
message produced from the Outbox.

Design the API so it remains independent from:

- Kafka
- Spring Kafka
- KafkaTemplate
- Avro
- transport-specific metadata

The abstraction should operate on the existing outbound message model.

Prefer names expressing business intent rather than transport terminology.

Examples of acceptable naming direction:

- OutboundMessagePublisher
- MessagePublisher

Avoid:

- KafkaPublisher
- KafkaProducer
- ProducerTemplate

--------------------------------------------------

Design Review

Before implementing, evaluate:

publish(...)

vs

send(...)

Choose the name that best expresses the architectural intent.

Document the rationale in the implementation report.

--------------------------------------------------

Validation

Ensure:

- Platform compiles.
- Existing modules compile without modification.
- No Kafka dependency is introduced.
- No Spring configuration changes are required.

--------------------------------------------------

Out of Scope

Do NOT implement:

- Kafka adapters
- KafkaTemplate
- ProducerFactory
- ConsumerFactory
- @KafkaListener
- Avro serialization
- Outbox drainer
- Scheduler
- Retry
- DLQ
- Topic resolution
- Runtime wiring
- Docker changes
- Testcontainers changes

Do not modify existing Application Services to use the new port.

--------------------------------------------------

Acceptance Criteria

✓ Runtime-neutral publishing interface exists.

✓ No Kafka dependency introduced.

✓ Platform remains transport independent.

✓ Existing build passes.

✓ Documentation updated if required.

--------------------------------------------------

Completion Report

Create:

docs/agents/reports/ARB-022.2.1-messaging-publisher-port.md

Follow the engineering-reporting protocol.

Include:

- rationale for the chosen interface name
- package location
- public API
- architectural intent
- validation performed

Do not begin ARB-022.2.2.