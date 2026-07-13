Task: ARB-022.2.4 — Publication Acknowledgement Contract

Status:
[PLANNED]

Owner:
Clio

Role:
Implementation

Timebox:
Target: 15–25 minutes
Hard stop: 45 minutes

Apply:

.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

If incomplete at the hard stop, create:

docs/agents/checkpoints/CHECKPOINT-ARB-022.2.4.md

and stop.

--------------------------------------------------

Context

ARB-022.2.1 introduced:

OutboundMessagePublisher

ARB-022.2.2 introduced:

KafkaOutboundMessagePublisher

ARB-022.2.3 introduced:

OutboundPayloadSerializer

The Kafka adapter delegates to:

KafkaTemplate.send(...)

which returns:

CompletableFuture<SendResult<K, V>>

However, the current publisher contract returns void and discards the
asynchronous acknowledgement.

The future Outbox Drainer must only call:

markPublished(...)

after Kafka confirms publication.

It must call:

markFailed(...)

when asynchronous publication fails.

This slice introduces the acknowledgement contract only.

Do not implement the Outbox Drainer.

--------------------------------------------------

Goal

Evolve OutboundMessagePublisher so callers can observe successful or failed
publication without exposing Kafka-specific types.

--------------------------------------------------

1. Publisher Contract

Change:

void publish(OutboxEvent message)

to:

CompletionStage<Void> publish(OutboxEvent message)

Use:

java.util.concurrent.CompletionStage

rather than:

CompletableFuture
SendResult
ListenableFuture
Kafka-specific result types

Rationale:

- CompletionStage expresses asynchronous completion.
- It is part of the JDK.
- It does not expose the transport implementation.
- Callers can coordinate success and failure.
- Implementations retain freedom over the concrete future type.

Document this rationale in the interface Javadoc and completion report.

--------------------------------------------------

2. Kafka Adapter

Update KafkaOutboundMessagePublisher.

Expected behavior:

KafkaTemplate.send(record)
↓
CompletableFuture<SendResult<String, String>>
↓
map successful completion to null
↓
return CompletionStage<Void>

Do not expose SendResult outside the Kafka adapter.

Do not block with:

get()
join()

Do not swallow exceptions.

Asynchronous Kafka failures must complete the returned stage exceptionally.

Immediate validation or routing failures may continue to throw directly before
a stage is returned.

--------------------------------------------------

3. Deprecated Compatibility Interface

Review the deprecated:

OutboxPublisher

Update it consistently with the new contract.

Do not introduce compatibility hacks that hide acknowledgement.

There are currently no production callers, so prefer correctness over preserving
the old void signature.

--------------------------------------------------

4. Tests

Update existing tests and add focused coverage for:

- successful Kafka send completes the returned stage successfully;
- failed Kafka send completes the returned stage exceptionally;
- publisher delegates exactly once to KafkaTemplate;
- routing failure still propagates and KafkaTemplate is not called;
- EVENT and COMMAND behavior remains unchanged;
- OutboundMessagePublisher remains lambda-friendly where practical.

Use completed and failed CompletableFuture instances.

No broker.
No Docker.
No Testcontainers.

--------------------------------------------------

5. Architecture Boundaries

Verify:

- OutboundMessagePublisher exposes no Kafka types;
- no SendResult leaks outside the Kafka adapter;
- domain and application layers remain Kafka-independent;
- CompletionStage is the only asynchronous abstraction in the public contract.

Do not add a custom result type unless a concrete need appears.

--------------------------------------------------

6. Documentation

Update:

docs/agents/reports/ARB-022.2.2-kafka-outbound-message-publisher-adapter.md

Only if needed to remove the now-resolved void-contract open question.

Create:

docs/agents/reports/ARB-022.2.4-publication-acknowledgement-contract.md

Document:

- contract evolution;
- why CompletionStage was selected;
- success semantics;
- failure semantics;
- why SendResult remains internal;
- validation executed.

--------------------------------------------------

Out of Scope

Do NOT implement:

- Outbox Drainer
- polling
- batching
- scheduler
- markPublished
- markFailed coordination
- retries
- backoff
- DLQ
- consumers
- Inbox processing
- Avro serialization
- Schema Registry
- JSON / Avro runtime selection
- blocking acknowledgement
- publication timeout policy

--------------------------------------------------

Acceptance Criteria

✓ OutboundMessagePublisher returns CompletionStage<Void>.

✓ Kafka SendResult remains internal to the Kafka adapter.

✓ Successful Kafka acknowledgement completes normally.

✓ Asynchronous Kafka failure completes exceptionally.

✓ No blocking call is introduced.

✓ Existing messaging behavior remains intact.

✓ Focused tests pass.

✓ Completion report exists.

Do not begin ARB-022.2.5.