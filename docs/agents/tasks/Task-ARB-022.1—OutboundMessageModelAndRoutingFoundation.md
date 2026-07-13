Task: ARB-022.1 — Outbound Message Model & Routing Foundation

Status:
[PLANNED]

Owner:
Clio

Timebox:
Apply the execution-timebox skill.

This task should normally complete within 20–30 minutes.
Hard stop at 45 minutes.

--------------------------------------------------

Goal

Extend the Outbox foundation so it can represent every reliable outbound
message produced by Arbitrier.

This slice defines the message model only.

Do NOT implement Kafka.

Do NOT implement producers.

Do NOT implement consumers.

--------------------------------------------------

Background

ARB-021 introduced the Outbox / Inbox persistence foundation.

Currently Outbox primarily stores Domain Events.

The next runtime slices will require both:

- Events
- Commands

to flow through the same reliable delivery mechanism.

This task establishes that abstraction.

--------------------------------------------------

Design Goals

Create a generic outbound message model.

The model must describe:

- what the message is
- why it exists
- where it should be routed

without exposing Kafka concepts.

--------------------------------------------------

Message Nature

Introduce:

MessageNature

Suggested values:

EVENT
COMMAND

Document why "nature" was chosen instead of "type" or "kind".

--------------------------------------------------

Routing Metadata

Review the current Outbox model.

Introduce (or refine if already present) routing metadata such as:

- messageNature
- logicalDestination
- messageName

These names may be refined if a better design emerges.

Avoid Kafka-specific terminology.

No topic names.

No partition information.

--------------------------------------------------

Outbound Routing Abstraction

Introduce an abstraction responsible for resolving:

OutboundMessage

↓

Runtime Destination

Example:

OutboundRoutingStrategy

or equivalent.

The implementation should remain runtime-independent.

Kafka will consume this abstraction later.

--------------------------------------------------

Message Mapping

Review DomainEvent → Outbox mapping.

Extend the design so future Commands can use the same mapping pipeline.

Do not yet implement command persistence.

Prepare the abstraction only.

--------------------------------------------------

Architecture

Keep dependency direction:

Application

↓

Platform Messaging

↓

Infrastructure

The domain must remain unaware of routing.

--------------------------------------------------

Documentation

Update:

ARB-021 documentation if necessary.

Create:

docs/implementation/ARB-022-1-outbound-message-model.md

Explain:

- why Outbox now models outbound messages instead of only events
- MessageNature
- routing abstraction
- future Kafka integration

--------------------------------------------------

Testing

Add focused tests covering:

- MessageNature
- routing metadata
- routing abstraction
- mapping consistency

No Kafka.

No Spring Kafka.

--------------------------------------------------

Out of Scope

- KafkaTemplate
- Producer
- Consumer
- Avro serialization
- Scheduler
- Polling
- Retry
- DLQ
- Inbox processing
- Runtime delivery

--------------------------------------------------

Acceptance Criteria

✔ MessageNature introduced.

✔ Outbox model supports future commands.

✔ Runtime-independent routing abstraction exists.

✔ No Kafka dependency introduced.

✔ Existing tests remain green.

✔ Documentation updated.

✔ Execution-timebox skill applied.

--------------------------------------------------

Completion Report

Report:

- architectural decisions taken
- new abstractions introduced
- files modified
- tests added
- documentation updated

Do not begin ARB-022.2.