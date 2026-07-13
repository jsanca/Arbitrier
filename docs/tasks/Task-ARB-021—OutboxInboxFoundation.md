Task: ARB-021 — Outbox / Inbox Foundation

Status:
[PLANNED]

Owner:
Clio

Context:

Arbitrier has completed:

- Domain Model
- Application Services
- Saga Orchestration
- Retry Policy
- Persistence Adapters
- Flyway Migrations
- Transaction Boundary Refinement
- Architecture Security Review

The next step is to establish reliable event persistence before introducing Kafka.

This slice introduces the Outbox/Inbox infrastructure only.

Kafka integration will be implemented in a later slice.

--------------------------------------------------

Goal

Provide a reusable transactional messaging foundation that guarantees
domain changes and event persistence occur atomically.

The objective is to prepare the platform for reliable asynchronous messaging.

Do NOT implement Kafka.

Do NOT implement scheduling.

Do NOT implement retries.

--------------------------------------------------

Architecture

The expected flow is:

Application Service

↓

Repository.save(aggregate)

↓

OutboxRepository.save(event)

↓

COMMIT

--------------------------------

Later:

OutboxPublisher

↓

Kafka

--------------------------------

Consumer

↓

InboxRepository

↓

Application Service

--------------------------------------------------

Platform

Create reusable platform components.

Suggested package:

platform

    messaging

        outbox

        inbox

The implementation must be generic.

Do not couple the platform to any bounded context.

--------------------------------------------------

Outbox Model

Introduce a generic OutboxEvent.

Suggested attributes:

- eventId
- aggregateId
- aggregateType
- eventType
- payload
- payloadFormat
- occurredAt
- publishedAt
- publishStatus
- attemptCount
- lastAttempt
- correlationId
- causationId

Do not expose Kafka concepts.

This represents a persisted business event.

--------------------------------------------------

Inbox Model

Introduce InboxEvent.

Suggested attributes:

- eventId
- consumerId
- receivedAt
- processedAt
- processingStatus
- correlationId
- payloadHash

Purpose:

Support future idempotent event processing.

--------------------------------------------------

Repository Ports

Create:

OutboxRepository

InboxRepository

Platform-owned.

Do not expose persistence technology.

--------------------------------------------------

Persistence

Implement JPA adapters.

Flyway migrations.

Optimistic locking where appropriate.

Indexes for:

Outbox

- publishStatus
- occurredAt
- aggregateId

Inbox

- consumerId
- processedAt
- eventId

--------------------------------------------------

Serialization

Introduce a serialization abstraction.

Suggested interface:

EventSerializer

Responsibilities:

serialize()

deserialize()

Initial implementation:

Jackson JSON.

Do not introduce Avro serialization here.

The payload should remain serializer-independent.

--------------------------------------------------

Domain Event Mapping

Domain Events must never know Outbox.

Create a mapper.

DomainEvent

↓

OutboxEvent

This mapping belongs outside the domain.

--------------------------------------------------

Application Services

Where application services currently publish events directly:

replace:

eventPublisher.publish(...)

with:

outboxRepository.save(...)

within the same transaction.

Do not publish asynchronously.

Persistence only.

--------------------------------------------------

Publisher Abstraction

Introduce:

OutboxPublisher

Platform interface only.

No implementation.

Kafka comes later.

--------------------------------------------------

Inbox Processing

Create only the persistence foundation.

Do not implement:

- polling
- deduplication workflow
- consumer orchestration

Those belong to future slices.

--------------------------------------------------

Flyway

Create migrations for:

platform.outbox_events

platform.inbox_events

Document ownership.

--------------------------------------------------

Testing

Add tests for:

- Outbox persistence
- Inbox persistence
- DomainEvent → Outbox mapping
- Serialization
- Transactional persistence

Required integration scenario:

save aggregate

↓

save outbox event

↓

commit

↓

verify both persisted

No Kafka.

--------------------------------------------------

Documentation

Create:

docs/implementation/ARB-021-outbox-inbox-foundation.md

Update:

README

Platform documentation

Architecture diagrams

Document:

- transactional messaging
- why Outbox exists
- why Inbox exists
- future Kafka integration
- relationship with Saga orchestration

--------------------------------------------------

Explicitly Out of Scope

Do NOT implement:

- Kafka Producer
- Kafka Consumer
- Spring Kafka
- Topic creation
- Scheduler
- Retry worker
- Dead Letter Queue
- Event replay
- Inbox cleanup
- Message ordering
- Distributed tracing
- Metrics
- Outbox polling

Those belong to future slices.

--------------------------------------------------

Acceptance Criteria

✔ Generic Outbox model.

✔ Generic Inbox model.

✔ Platform-owned abstractions.

✔ JPA persistence.

✔ Flyway migrations.

✔ Repository ports.

✔ Serialization abstraction.

✔ DomainEvent mapping.

✔ Aggregate + Outbox persisted atomically.

✔ Integration tests.

✔ Documentation.

✔ Existing tests remain green.

--------------------------------------------------

After completion

Report:

- created entities
- repository ports
- JPA adapters
- migrations
- serialization design
- mapping strategy
- tests added
- documentation updated
- open questions

Do not implement Kafka.

Do not begin ARB-022.