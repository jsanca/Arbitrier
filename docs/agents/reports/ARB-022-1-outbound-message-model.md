# ARB-022.1 — Outbound Message Model & Routing Foundation

## Why the Outbox now models outbound messages, not only events

The original outbox modelled only domain events. Every message stored was assumed to be a fact — something that had already happened — and named with an event-style class name (e.g. `OrderCreatedDomainEvent`).

Future slices require the orchestrator to issue **commands** — directed instructions to specific services (e.g. `ReserveStockCommand`, `ReleaseCreditCommand`). Commands must be written to the outbox atomically with the saga state change, ensuring eventual at-least-once delivery; idempotent processing at the recipient guards against duplicates. The outbox is the correct mechanism.

Rather than creating a separate `outbox_commands` table (which would duplicate all lifecycle columns), we extended the single `outbox_events` table to carry any outbound message, discriminated by nature.

---

## MessageNature

```java
public enum MessageNature { EVENT, COMMAND }
```

The field name `nature` was chosen instead of `type` (collision with `eventType`, the message name field) or `kind` (informal; does not communicate the protocol role). *Nature* conveys the structural role a message plays in the protocol:

- **EVENT** — a record of something that has already happened. Recipients may react but are not obligated to.
- **COMMAND** — a directed instruction for a specific recipient to perform a specific action.

`messageNature` is stored in the `outbox_events` table as a VARCHAR with a check constraint. Existing rows default to `'EVENT'` via the V3 migration.

---

## Routing Abstraction

```java
public interface OutboundRoutingStrategy {
    String resolveDestination(OutboxEvent message);
}
```

`OutboundRoutingStrategy` decouples the outbox domain from transport infrastructure. It accepts an `OutboxEvent` and returns a **logical destination** — a domain-level routing identifier such as `"order.created"` or `"credit.commands"` — with no knowledge of Kafka topics, partitions, or transport addresses.

The transport layer (a Kafka publisher, added in a later slice) will implement this interface and translate logical destinations to physical Kafka topic names. The outbox domain never imports Kafka.

Implementations can inspect `messageNature` to route events and commands to different logical channels, and `eventType` to route specific message types within a channel.

---

## Mapper Changes

`DomainEventToOutboxMapper` now provides two overloads:

| Overload | Nature set | Use case |
|----------|-----------|----------|
| `map(Object, String, String)` | `EVENT` | All existing domain event callers — unchanged |
| `map(Object, String, String, MessageNature)` | caller-supplied | Commands and future message types |

The three-argument overload delegates to the four-argument one, so existing callers in all services require no changes.

---

## Future Kafka Integration

When the Kafka publisher slice (ARB-022.x) is implemented:

1. Implement `OutboundRoutingStrategy` in the Kafka adapter layer, mapping logical destinations to Kafka topic names from `TopicNames`.
2. The outbox poller reads `OutboxEvent.messageNature` to apply nature-specific serialisation or header conventions if needed.
3. No changes to the platform model or any application service are required.

---

## Database Migration

`platform/V3__add_outbox_routing_metadata.sql` adds:

```sql
ALTER TABLE outbox_events
    ADD COLUMN message_nature VARCHAR(20) NOT NULL DEFAULT 'EVENT';

ALTER TABLE outbox_events
    ADD CONSTRAINT outbox_message_nature_chk
        CHECK (message_nature IN ('EVENT', 'COMMAND'));
```

Runs in every service schema (order_service, inventory_service, credit_service, orchestrator_service) via the platform Flyway location.

---

## Files Changed

### Platform — production

| File | Change |
|------|--------|
| `platform/.../outbox/MessageNature.java` | New enum (EVENT, COMMAND) |
| `platform/.../outbox/OutboundRoutingStrategy.java` | New interface |
| `platform/.../outbox/OutboxEvent.java` | Added `messageNature` field (14th, last position) with `Require.notNull` guard |
| `platform/.../outbox/adapter/OutboxEventEntity.java` | Added `message_nature` VARCHAR column + getter/setter |
| `platform/.../outbox/adapter/JpaOutboxRepositoryAdapter.java` | Wire `messageNature` in `toEntity` / `toRecord` |
| `platform/.../outbox/mapper/DomainEventToOutboxMapper.java` | New 4-arg overload; 3-arg delegates with `MessageNature.EVENT` |
| `platform/.../messaging/test/InMemoryOutboxRepository.java` | Preserve `messageNature` in `markPublished` / `markFailed` lambdas |
| `platform/src/main/resources/db/migration/platform/V3__add_outbox_routing_metadata.sql` | New migration — adds `message_nature` column + check constraint |

### Platform — tests

| File | Change |
|------|--------|
| `platform/.../outbox/OutboxEventTest.java` | All 13 constructors updated; added `creates_command_message`, `null_messageNature_throws` |
| `platform/.../outbox/MessageNatureTest.java` | New — 3 tests (values exist, valueOf roundtrip, EVENT is not COMMAND) |
| `platform/.../outbox/OutboundRoutingStrategyTest.java` | New — 2 tests (resolves by eventType, discriminates by nature) |
| `platform/.../outbox/mapper/DomainEventToOutboxMapperTest.java` | Added `three_arg_overload_always_produces_event_nature`, `four_arg_overload_maps_command_nature`, `four_arg_null_nature_throws`; fixed `null_message_throws` expected string |
| `platform/.../messaging/test/InMemoryOutboxRepositoryTest.java` | All constructor calls updated with `MessageNature.EVENT` |
| `platform/.../outbox/adapter/JpaOutboxRepositoryAdapterTest.java` | `createPendingEvent()` updated with `MessageNature.EVENT` |

### Service persistence configurations (entity scanning fix)

During implementation, a pre-existing bug was discovered: `OutboxEventEntity` and `InboxEventEntity` were not being scanned in Testcontainer JPA adapter tests because `spring.jpa.packages` in `application.yml` is not effective when a test boots an inner `@SpringBootConfiguration`. Fixed by adding explicit `@EntityScan` to all four service persistence configurations using the correct Spring Boot 4.1 import (`org.springframework.boot.persistence.autoconfigure.EntityScan`).

| File | Change |
|------|--------|
| `order-service/.../config/OrderPersistenceConfiguration.java` | Added `@EntityScan` with `OutboxEventEntity` + `InboxEventEntity` |
| `inventory-service/.../config/InventoryPersistenceConfiguration.java` | Same |
| `credit-service/.../config/CreditPersistenceConfiguration.java` | Same |
| `orchestrator-service/.../config/OrchestratorPersistenceConfiguration.java` | Same |

### Documentation

| File | Change |
|------|--------|
| `docs/implementation/ARB-022-1-outbound-message-model.md` | This document |
| `CLAUDE.md` | Added `@EntityScan` package-location note; added `MessageNature` and `OutboundRoutingStrategy` to Platform Utilities table |

---

## Validation

Full test suite: **232 tests, 0 failures, 0 errors** across all 6 modules (contracts, platform, order-service, inventory-service, credit-service, orchestrator-service). Run: `mvn -B test --no-transfer-progress -pl server/contracts,server/platform,server/order-service,server/inventory-service,server/credit-service,server/orchestrator-service`.

---

## Out of Scope

Kafka, producers, consumers, `logicalDestination` as a persisted field, scheduler, retry, DLQ. These belong to later ARB-022.x slices.
