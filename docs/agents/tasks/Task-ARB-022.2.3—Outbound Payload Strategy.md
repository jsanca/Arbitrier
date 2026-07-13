Task: ARB-022.2.3 — Outbound Payload Strategy

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

--------------------------------------------------

Context

ARB-022.2.2 introduced the first Kafka transport adapter using
KafkaTemplate<String, String>.

Architecture review concluded:

- JSON is appropriate for development.
- Avro should become the production transport.
- The transport abstraction should remain unchanged.

Before implementing the Outbox Drainer, the runtime requires a transport
serialization strategy.

This slice introduces only that abstraction.

No Avro serialization is implemented.

--------------------------------------------------

Goal

Introduce a runtime-neutral outbound payload serialization strategy that allows
multiple transport implementations without modifying
OutboundMessagePublisher.

The strategy defines how an OutboxEvent becomes the transport payload.

--------------------------------------------------

Requirements

Introduce a new Platform abstraction.

Suggested naming direction:

OutboundPayloadSerializer

or

OutboundMessageSerializer

The abstraction should accept:

OutboxEvent

and produce a transport payload suitable for the active publisher.

Do not expose Kafka types.

Do not expose Avro types.

The abstraction must remain transport-neutral.

--------------------------------------------------

Development Serializer

Implement the current JSON behaviour as the first serializer.

Suggested implementation:

JsonOutboundPayloadSerializer

The implementation should preserve today's behaviour.

No functional changes.

--------------------------------------------------

Architecture

The serializer is injected into the transport adapter.

Expected flow:

OutboxEvent
↓
OutboundPayloadSerializer
↓
serialized payload
↓
KafkaOutboundMessagePublisher
↓
KafkaTemplate

--------------------------------------------------

Do NOT implement

- Avro serializer
- Schema Registry
- SpecificRecord
- KafkaAvroSerializer
- TopicNameResolver
- Outbox Drainer
- Scheduler
- Retry
- Consumer
- Inbox
- Runtime profile switching

--------------------------------------------------

Tests

Add focused unit tests covering:

- JSON serializer output
- null validation
- publisher delegates serialization to the serializer

Do not add broker tests.

--------------------------------------------------

Documentation

Create:

docs/agents/reports/ARB-022.2.3-outbound-payload-strategy.md

Document:

- rationale
- abstraction
- current JSON implementation
- why Avro is intentionally deferred
- future extension points

--------------------------------------------------

Acceptance Criteria

✓ Transport-neutral serializer abstraction exists.

✓ JSON implementation preserves current behaviour.

✓ Kafka publisher depends on serializer abstraction.

✓ No Avro code introduced.

✓ Existing build passes.

✓ Targeted tests pass.

Do not begin Avro serialization.

Do not begin the Outbox Drainer.