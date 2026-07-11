Task: ARB-014 — Saga Orchestrator

Status:
[COMPLETE]

Owner:
Clio

Context:
ARB-012 Inventory Service is DONE.
ARB-013 Credit Service is DONE.

The Order, Inventory and Credit bounded contexts already expose clean
application services, domain events and contracts.

ARB-014 introduces the Saga Orchestrator infrastructure only.

This slice creates the Saga orchestration engine and persistence model,
but DOES NOT wire Kafka consumers, Kafka producers or execute the full
business workflow.

Goal:
Implement the Saga Orchestrator foundation:
- Saga lifecycle
- Saga persistence
- Saga commands/events
- Saga state transitions
- Application services

Do NOT implement the Happy Path yet.

Scope:
server/orchestrator-service only.

In scope:

1. Inbound application ports

- StartSagaUseCase
- AdvanceSagaUseCase
- CompensateSagaUseCase

2. Application commands

- StartSagaCommand
- AdvanceSagaCommand
- CompensateSagaCommand

3. Application results

- StartSagaResult
- AdvanceSagaResult
- CompensateSagaResult

4. Application services

- StartSagaService
- AdvanceSagaService
- CompensateSagaService

Application services should follow the established grammar:

validate
↓

load/create aggregate
↓

apply domain transition
↓

persist
↓

publish domain event
↓

return result

5. Outbound ports

- SagaRepository
- SagaEventPublisher

6. Domain events

- SagaStartedDomainEvent
- SagaAdvancedDomainEvent
- SagaCompensatedDomainEvent

Domain events must remain pure Java.

7. Test adapters

- InMemorySagaRepository
- RecordingSagaEventPublisher

Functional behaviour

StartSagaUseCase

Input:

- sagaId
- orderId
- customerId

Behaviour:

- create Saga aggregate
- initial status:
  STARTED
- initial step:
  ORDER_CREATED
- persist
- publish SagaStartedDomainEvent
- return result

AdvanceSagaUseCase

Input:

- sagaId
- nextStep

Behaviour:

- load Saga
- advance to requested step
- persist
- publish SagaAdvancedDomainEvent
- return result

Do NOT execute Inventory or Credit.

Only update Saga state.

CompensateSagaUseCase

Input:

- sagaId

Behaviour:

- load Saga
- mark Saga as COMPENSATING
- persist
- publish SagaCompensatedDomainEvent
- return result

Do NOT execute compensating commands.

Compensation wiring belongs to ARB-016.

Out of scope

- No Kafka consumer.
- No Kafka producer.
- No Avro mapper.
- No REST controller.
- No JPA.
- No Postgres.
- No Flyway.
- No Inventory invocation.
- No Credit invocation.
- No Order invocation.
- No Happy Path.
- No Compensation workflow.
- No timeouts.
- No retries.
- No scheduler.
- No security integration.
- No RuntimeHints.

Architecture rules

- Domain remains pure Java.
- Application does not depend on Kafka/JPA/REST.
- Saga aggregate owns its lifecycle.
- Domain events are pure Java.
- No generated Avro classes.
- No infrastructure leakage.

Logging

Use SLF4J.

Log only:

- sagaId
- orderId
- currentStep
- currentStatus

Never log PII.

Transactionality

Do not introduce @Transactional.

Document:

Future JPA phase will introduce transaction boundaries.

Outbox will guarantee DB + Kafka consistency.

Native Image

Avoid:

- reflection
- runtime scanning
- dynamic proxies
- Class.forName()

No RuntimeHints yet.

Tests

Cover:

- create saga
- duplicate saga validation if applicable
- advance saga
- invalid transition
- compensate saga
- repository save
- event publication
- validation failures

Architecture tests:

- domain -> adapter
- application -> adapter
- domain -> Spring/JPA
- domain -> Kafka/Avro
- application -> Kafka/Avro

Documentation

Create:

docs/implementation/ARB-014-saga-orchestrator.md

Update if needed:

server/orchestrator-service/README.md

docs/okf/index.md

Acceptance Criteria

- orchestrator-service compiles.
- tests pass without Docker, Kafka, Postgres, Keycloak or Schema Registry.
- SagaRepository exists.
- SagaEventPublisher exists.
- StartSagaUseCase exists.
- AdvanceSagaUseCase exists.
- CompensateSagaUseCase exists.
- Domain events are pure Java.
- No Kafka runtime.
- No Avro usage.
- No JPA.
- No business orchestration yet.
- Documentation captures remaining OPEN QUESTIONS.
- Ready for Deep review.

Important

ARB-014 builds the orchestration engine.

ARB-015 will connect the happy-path workflow.

ARB-016 will connect compensation.

Do not implement ARB-015 or ARB-016.
