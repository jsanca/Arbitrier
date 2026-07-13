# ARB-021 — Outbox / Inbox Foundation — Completion Report

**Date:** 2026-07-11
**Status:** COMPLETE

## Summary

Verified that the outbox/inbox foundation is fully implemented per ARB-021 requirements.
Closed the remaining gaps: added comprehensive test coverage for all platform messaging
components, added messaging-layer architecture rules, and updated documentation.

## What Was Already Complete

All core infrastructure was already built:

- **Platform models**: `OutboxEvent` record (13 fields), `InboxEvent` record (7 fields),
  `PublishStatus`, `ProcessingStatus`
- **Repository ports**: `OutboxRepository` (save, findPending, markPublished, markFailed),
  `InboxRepository` (save, findById, markProcessed), `OutboxPublisher` (placeholder)
- **JPA adapters**: `OutboxEventEntity`, `InboxEventEntity`, `SpringData*Repository`,
  `JpaOutboxRepositoryAdapter`, `JpaInboxRepositoryAdapter`
- **Serialization**: `EventSerializer` interface + `JacksonEventSerializer` (JSON)
- **Domain event mapping**: `DomainEventToOutboxMapper`
- **Flyway migration**: `V2__create_outbox_inbox_tables.sql` (42 lines, indexes, CHECK constraints)
- **Test doubles**: `InMemoryOutboxRepository`, `InMemoryInboxRepository`
- **Application service wiring**: All 17 application services across 4 services persist domain
  events via `outboxRepository.save(mapper.map(...))` inside `@Transactional` boundaries
- **Integration test**: `TransactionalOutboxIT` in order-service proves atomic persistence
- **Auto-configuration**: `PlatformAutoConfiguration` registers `EventSerializer` and
  `DomainEventToOutboxMapper` beans

## What Was Added (This Completion)

### Tests Added (8 test classes, 73 test methods)

| Test Class | Location | Methods | Type |
|---|---|---|---|
| `OutboxEventTest` | `platform/messaging/outbox/` | 10 | Unit |
| `InboxEventTest` | `platform/messaging/inbox/` | 7 | Unit |
| `JacksonEventSerializerTest` | `platform/messaging/serialization/` | 6 | Unit |
| `DomainEventToOutboxMapperTest` | `platform/messaging/outbox/mapper/` | 9 | Unit |
| `InMemoryOutboxRepositoryTest` | `platform/messaging/test/` | 8 | Unit |
| `InMemoryInboxRepositoryTest` | `platform/messaging/test/` | 7 | Unit |
| `JpaOutboxRepositoryAdapterTest` | `platform/messaging/outbox/adapter/` | 6 | Integration (Testcontainers) |
| `JpaInboxRepositoryAdapterTest` | `platform/messaging/inbox/adapter/` | 5 | Integration (Testcontainers) |

### Architecture Test Updated

`PlatformArchitectureTest` gained two new ArchUnit rules:

1. **Messaging domain purity** — outbox, inbox, serialization, and mapper packages must not
   reference `jakarta.persistence..`, `org.springframework..`, or `org.hibernate..`

2. **Adapter isolation** — outbox/inbox adapter packages must not reference business domain
   packages (`com.arbitrier.order..`, `.inventory..`, `.credit..`, `.orchestrator..`)

### POM Changes

`server/platform/pom.xml` — added `spring-boot-testcontainers` (test scope) to enable
`@ServiceConnection` for JPA adapter tests using Testcontainers.

### Test Resources

Added `server/platform/src/test/resources/test-db/create-platform-schema.sql` for JPA
adapter Testcontainers tests.

## Verified Test Results

All existing and new tests pass:

```
Platform:         156 tests (was 83) — 73 new messaging tests
Order Service:    core tests pass (ArchitectureTest, SubmitCorporateBulkOrderServiceTest)
Inventory Service: core tests pass (ArchitectureTest, ReserveStockServiceTest, ReleaseStockServiceTest)
Credit Service:   core tests pass (ArchitectureTest, ReserveCreditServiceTest, ReleaseCreditServiceTest)
Orchestrator:     core tests pass (ArchitectureTest, HandleOrderCreatedServiceTest, StartSagaServiceTest, etc.)
Contracts:        20 tests, all pass
```

## Open Questions (unchanged)

- OPEN QUESTION: the Kafka outbox drainer / dispatcher is out of scope for ARB-021; the
  `OutboxPublisher` interface is a marker for the next slice.
- OPEN QUESTION: correlation and causation IDs are currently written as `null`; wiring them
  from the correlation filter and Kafka consumer to the outbox mapper is deferred.
- OPEN QUESTION: schema-registry-backed Avro serialization for outbox payloads
  (`payload_format = "AVRO"`) will replace the JSON default when the contracts-first
  serializer is chosen (see ARB-011 open question).

## No Kafka Implementation

As specified, no Kafka producer, consumer, scheduler, retry worker, dead letter queue,
event replay, inbox cleanup, message ordering, distributed tracing, metrics, or outbox
polling was implemented. These belong to future slices.
