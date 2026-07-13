Task: ARB-022.2.2-FIX-001 — Kafka Publisher Review Fixes

Status:
[PLANNED]

Owner:
Clio

Role:
Implementation

Timebox:
Target: 15–20 minutes

Hard stop:
45 minutes

Apply:

.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

If interrupted:

Create:

docs/agents/checkpoints/CHECKPOINT-ARB-022.2.2-FIX-001.md

--------------------------------------------------

Context

ARB-022.2.2 received PASS WITH WARNINGS.

The architecture is accepted.

Only the agreed review findings are implemented here.

Do not redesign the messaging architecture.

Do not introduce Avro.

Do not modify the publisher contract.

--------------------------------------------------

Accepted Review Findings

A2
Missing @ConditionalOnBean(OutboundRoutingStrategy.class)

H2
Missing ArchUnit protection preventing Kafka types from leaking into
platform.messaging.outbox / inbox.

R2
Javadoc currently describes "logical destination" while the adapter uses the
returned value directly as the Kafka topic.

T2
Missing routing-strategy failure propagation test.

--------------------------------------------------

1.

KafkaPublisherAutoConfiguration

Add:

@ConditionalOnBean(OutboundRoutingStrategy.class)

to the publisher bean.

The publisher should only exist when a routing strategy exists.

Do not create a default routing strategy.

--------------------------------------------------

2.

PlatformArchitectureTest

Add an ArchUnit rule preventing:

platform.messaging.outbox
platform.messaging.inbox

from depending on:

org.springframework.kafka..

or

org.apache.kafka..

The rule should mirror the existing ObjectMapper confinement philosophy.

--------------------------------------------------

3.

OutboundRoutingStrategy

Review the current Javadoc.

The implementation currently uses the returned String directly as the Kafka
topic.

Update the documentation so it accurately reflects the current behavior.

Do not introduce TopicNameResolver.

Do not introduce logical-to-physical translation.

Future evolution can revisit this abstraction if required.

--------------------------------------------------

4.

KafkaOutboundMessagePublisherTest

Add one focused test.

Scenario:

routingStrategy.resolveDestination(...)

throws an exception.

Verify:

- publish() propagates the exception.
- kafkaTemplate.send(...) is never invoked.

No additional broker tests.

--------------------------------------------------

Out of Scope

Do NOT:

- introduce Avro serialization
- create Json/Avro adapters
- redesign OutboundRoutingStrategy
- modify OutboundMessagePublisher
- change publish() return type
- implement Outbox Drainer
- introduce TopicNameResolver
- implement Schema Registry
- modify roadmap

--------------------------------------------------

Documentation

Update the implementation report if needed.

Do not rewrite the architecture review.

Create:

docs/agents/reports/ARB-022.2.2-FIX-001-kafka-publisher-review-fixes.md

--------------------------------------------------

Acceptance Criteria

✓ Publisher bean is conditional on OutboundRoutingStrategy.

✓ ArchUnit protects messaging contracts from Kafka dependencies.

✓ Javadoc matches current implementation.

✓ Routing failure propagation test exists.

✓ Build passes.

✓ Targeted tests pass.

Do not begin ARB-022.2.3.