Task: ARB-022.2.2 — Kafka Outbound Message Publisher Adapter

Status:
[PLANNED]

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

If the hard stop is reached, create:

docs/agents/checkpoints/CHECKPOINT-ARB-022.2.2.md

and stop.

--------------------------------------------------

Context

ARB-022.2.1 introduced:

OutboundMessagePublisher.publish(OutboxEvent)

as the runtime-neutral publication port.

ARB-022.1 introduced:

- OutboxEvent
- MessageNature
- OutboundRoutingStrategy

This slice implements the first transport adapter using Spring Kafka.

It does not read the Outbox.
It does not update Outbox status.
It does not consume messages.

--------------------------------------------------

Goal

Implement OutboundMessagePublisher using KafkaTemplate.

Expected flow:

OutboxEvent
    ↓
OutboundRoutingStrategy.resolveDestination(...)
    ↓
KafkaTemplate.send(...)
```

The adapter must remain outside the application and domain layers.

--------------------------------------------------

1. Adapter

Create a Kafka adapter implementing:

OutboundMessagePublisher

Suggested name:

KafkaOutboundMessagePublisher

Suggested package direction:

com.arbitrier.platform.messaging.kafka.outbound

or the existing equivalent Kafka adapter package if one already establishes
the project convention.

Do not place Kafka imports in:

- domain
- application
- outbox model
- OutboundMessagePublisher
- OutboundRoutingStrategy

--------------------------------------------------

2. Publication Contract

The adapter receives an OutboxEvent and must:

1. Validate the message is non-null.
2. Resolve its destination through OutboundRoutingStrategy.
3. Publish its existing serialized payload through KafkaTemplate.
4. Use a stable message key.

Preferred key:

aggregateId

If aggregateId is unsuitable in the actual implementation, use eventId and
document the rationale.

Do not deserialize and reserialize the payload.

The Outbox payload is already serialized.

--------------------------------------------------

3. Kafka Value Type

For this slice, use the current persisted payload representation.

Expected direction:

KafkaTemplate<String, String>

Do not introduce Avro serialization yet.

Avro mapping and Schema Registry belong to ARB-022.2.x later slices.

The adapter should publish the payload exactly as stored in OutboxEvent.

--------------------------------------------------

4. Headers

Add only metadata already present and useful for later consumers.

Where values are available, propagate:

- event/message ID
- message name/type
- message nature
- aggregate ID
- aggregate type
- correlation ID
- causation ID
- payload format

Use existing platform header constants where appropriate.

Do not invent Kafka-specific metadata in the Outbox domain model.

Do not add null-valued headers.

--------------------------------------------------

5. Asynchronous Result

KafkaTemplate.send(...) is asynchronous.

The OutboundMessagePublisher contract currently returns void.

For this slice:

- initiate publication;
- propagate immediate invocation/configuration failures;
- do not mark the message as published;
- do not mark it as failed;
- do not block waiting for broker acknowledgement unless the existing project
  convention explicitly requires it.

The future drainer owns success/failure coordination and Outbox status updates.

If the void contract makes correct delivery acknowledgement impossible,
document the concern as an open question instead of redesigning the port in
this slice.

--------------------------------------------------

6. Configuration

Register the adapter as a Spring bean only when the required Kafka types and
dependencies are present.

Prefer conditional configuration consistent with existing Platform
auto-configuration patterns.

Do not add:

- scheduler configuration
- polling configuration
- retry configuration
- DLQ configuration
- consumer configuration

Reuse existing Spring Kafka dependencies if already present.

Add the minimum dependency only if genuinely absent.

--------------------------------------------------

7. Existing Kafka Publishers

The repository may already contain service-specific Kafka event publisher
adapters.

Do not delete or broadly refactor them.

Do not wire application services to the new adapter.

Only reuse conventions or utilities when doing so is small and safe.

Record obsolete or duplicative adapters as a future cleanup note.

--------------------------------------------------

8. Tests

Add focused unit tests for KafkaOutboundMessagePublisher.

Cover at least:

- resolves destination through OutboundRoutingStrategy;
- publishes the stored payload unchanged;
- uses the expected key;
- propagates EVENT nature metadata;
- propagates COMMAND nature metadata;
- omits null correlation/causation headers;
- rejects null OutboxEvent;
- KafkaTemplate is invoked exactly once.

Mock or fake KafkaTemplate and the routing strategy.

No broker.
No Docker.
No Testcontainers.
No Kafka integration test in this slice.

--------------------------------------------------

9. Architecture Validation

Verify:

- no Kafka dependency leaks into domain/application packages;
- OutboundMessagePublisher remains transport-neutral;
- OutboundRoutingStrategy remains free of Kafka types;
- the adapter depends inward on platform messaging contracts.

Add or update one architecture rule only if the current suite does not already
protect this boundary.

Avoid broad test expansion.

--------------------------------------------------

10. Documentation

Create:

docs/agents/reports/ARB-022.2.2-kafka-outbound-message-publisher-adapter.md

Use the engineering-reporting protocol.

Document:

- adapter package and bean wiring;
- KafkaTemplate generic types;
- key choice;
- destination resolution;
- headers propagated;
- asynchronous acknowledgement limitation;
- tests executed;
- open questions.

Update canonical implementation documentation only when required to keep it
accurate.

--------------------------------------------------

Out of Scope

Do NOT implement:

- Outbox drainer
- database polling
- markPublished / markFailed coordination
- scheduler
- retries or backoff
- DLQ
- Kafka consumers
- Inbox processing
- Avro mapping
- Schema Registry integration
- command publisher replacement
- application-service wiring
- logicalDestination persistence
- topic provisioning
- Docker Compose changes

--------------------------------------------------

Acceptance Criteria

✓ KafkaOutboundMessagePublisher implements OutboundMessagePublisher.

✓ Destination is resolved through OutboundRoutingStrategy.

✓ Existing serialized payload is published unchanged.

✓ Stable Kafka key is used and documented.

✓ Available metadata headers are propagated.

✓ No Kafka types leak into platform messaging contracts.

✓ No Outbox polling or status updates are introduced.

✓ Focused unit tests pass.

✓ Existing affected modules compile and test successfully.

✓ Completion report exists.

Do not begin ARB-022.2.3.
```
