# contracts

Shared Avro wire contracts for UC-01 messaging. The module contains 26 `.avsc` schemas across common, order, inventory, credit, and orchestrator namespaces. The Avro Maven plugin generates Java sources during the build; generated sources are not committed.

## Contract families

| Namespace | Examples |
|---|---|
| `common` | `MessageMetadata`, `MoneyAmount`, `OrderLineContract`, `CancellationReason` |
| `order` | `OrderCreated`, `OrderConfirmed`, `OrderPartiallyConfirmed`, `OrderCancelled` |
| `inventory` | reserve/release requests and stock reserved/partial/rejected/released outcomes |
| `credit` | reserve/release requests and credit approved/rejected/released outcomes |
| `orchestrator` | saga completed/cancelled/compensation failed and customer-decision contracts retained for contract evolution |

Schema files are versioned contracts even though the current v1 filenames omit a `-v1` suffix. Breaking evolution requires a new schema/subject version and coordinated consumers. The local Schema Registry defaults to `BACKWARD` compatibility and future producers should use `TopicNameStrategy` (`<topic>-value`).

```bash
mvn -B generate-sources --no-transfer-progress -pl server/contracts
mvn -B test --no-transfer-progress -pl server/contracts
```

## Runtime boundary

ARB-011 established the order publisher foundation and canonical header conventions. Most Kafka producers/consumers, final topic provisioning, Confluent `KafkaAvroSerializer`, and live compatibility checks remain ARB-022/023 work. The schemas are implemented; complete production messaging is not.

See [ADR-0004](../../docs/adr/ADR-0004-avro-contracts-and-schema-registry.md) and the [local runtime guide](../../infra/docker/README.md).
