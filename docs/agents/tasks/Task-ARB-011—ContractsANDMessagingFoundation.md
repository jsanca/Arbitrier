Task: ARB-011 — Contracts & Messaging Foundation

Status:
[COMPLETE]

Owner:
Clio

Context:
ARB-006 Domain Contracts is DONE.
ARB-007 Order Service is DONE.
ARB-009 Observability Foundation is DONE.
ARB-010 Security Integration is DONE.

The order-service currently publishes OrderCreatedDomainEvent through the OrderEventPublisher outbound port, but the only implementation is test/recording infrastructure.
ARB-011 introduces the first real messaging foundation and maps domain events to Avro contracts.

Goal:
Implement the messaging foundation needed for Kafka + Avro publishing, without implementing the full saga or any consumers.

Primary slice:
OrderCreatedDomainEvent
→ OrderCreated Avro contract
→ KafkaOrderEventPublisher
→ configured topic name

Modules in scope:
- server/platform
- server/order-service
- server/contracts if minor package/build adjustments are needed

In scope:

1. Topic naming conventions
- Add a central topic naming class/config.
- Define at least:
    - arbitrier.order.created.v1
- Optionally define placeholders for future topics:
    - arbitrier.stock.reserved.v1
    - arbitrier.stock.partially-reserved.v1
    - arbitrier.stock.rejected.v1
    - arbitrier.credit.approved.v1
    - arbitrier.credit.rejected.v1
- Do not implement consumers for these topics.

2. Kafka message header conventions
   Define constants and helper behavior for:
- messageId
- correlationId
- causationId
- traceparent
- tracestate
- schemaVersion

Important:
- correlationId is business traceability.
- traceparent/tracestate follow W3C Trace Context.
- B3 must not be introduced.

3. Order Avro mapper
   Create a mapper in order-service adapter layer:
- OrderCreatedDomainEvent → com.arbitrier.contracts.order.OrderCreated

Mapping rules:
- orderId → string
- customerId → string
- submittedByUserId → string
- lines → OrderLineContract list
- metadata → MessageMetadata

requestedTotal:
- ARB-006 left requestedTotal source of truth unresolved.
- For this slice, do not invent pricing.
- Choose one:
  a) use MoneyAmount("0", "USD") only if documented as temporary test/default value, or
  b) update mapper to require requestedTotal as input from application layer, or
  c) defer KafkaOrderEventPublisher until requestedTotal is resolved.
- Prefer the smallest clean option, and document the decision clearly.

4. Kafka publisher adapter
   Implement:
- KafkaOrderEventPublisher implements OrderEventPublisher

Behavior:
- maps OrderCreatedDomainEvent to Avro OrderCreated
- sends through KafkaTemplate
- uses configured topic name
- attaches headers where appropriate
- logs safe IDs only

5. Configuration
- Add Spring Kafka dependencies to order-service only.
- Add Avro serializer dependency/config if needed.
- Add application.yml placeholders:
    - spring.kafka.bootstrap-servers
    - schema registry URL if needed
    - topic name property
- Avoid requiring Kafka to run unit tests.

6. Tests
- Mapper test:
    - maps orderId/customerId/userId/lines correctly
    - creates metadata
    - does not use tenantId
    - handles requestedTotal decision as documented

- Publisher test:
    - uses KafkaTemplate mock
    - sends to expected topic
    - sends Avro OrderCreated payload
    - adds expected headers
    - does not require Kafka broker

- Architecture tests:
    - domain does not depend on Avro/Kafka
    - application does not depend on Avro/Kafka
    - Kafka adapter may depend on Avro/Kafka

Out of scope:
- No Kafka consumers.
- No inventory-service integration.
- No credit-service integration.
- No orchestrator-service integration.
- No saga orchestration.
- No JPA.
- No Outbox/Inbox.
- No transaction coordination.
- No real Schema Registry test.
- No Testcontainers unless explicitly needed.
- No native image build.
- No B3 propagation.

Native Image:
- Document Avro/Kafka reflection/native-image concerns.
- Do not add RuntimeHints unless immediately necessary.
- Keep generated Avro classes isolated to adapter/integration code.

Documentation:
Create:
- docs/implementation/ARB-011-contracts-messaging-foundation.md

Update:
- docs/adr/ADR-0004-avro-contracts-and-schema-registry.md if topic naming/schema registry decisions are clarified
- server/order-service/README.md
- server/platform/README.md if shared Kafka header constants are added
- docs/okf/index.md if needed

Acceptance Criteria:
- order-service compiles.
- contracts module still compiles.
- tests pass without Kafka broker, Schema Registry, Postgres, Keycloak, or Docker.
- OrderCreatedDomainEvent maps to Avro OrderCreated.
- KafkaOrderEventPublisher exists.
- Kafka runtime is introduced only in outbound adapter/config, not domain/application.
- Domain remains Kafka/Avro-free.
- Application remains Kafka/Avro-free.
- W3C Trace Context is preserved; B3 is not introduced.
- requestedTotal decision is explicitly documented.
- ARB-011 is ready for Deep review.

After completion:
- Report created files.
- Report tests run.
- Report open questions.
- Do not start ARB-012.
