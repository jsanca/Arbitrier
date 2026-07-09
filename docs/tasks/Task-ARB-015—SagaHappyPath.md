Task: ARB-015 — Saga Happy Path

Status:
[PLANNED]

Owner:
Clio

Context:
ARB-014 Saga Orchestrator is DONE.

The Saga foundation already exists:
- StartSagaUseCase
- AdvanceSagaUseCase
- CompensateSagaUseCase
- Saga aggregate
- SagaRepository
- SagaEventPublisher

Inventory Service and Credit Service already expose clean application services.

ARB-015 wires together the successful business flow only.

Goal:
Implement the Happy Path orchestration for UC-01 Corporate Bulk Order.

Happy Path:

OrderCreated
↓
Start Saga
↓
Reserve Inventory
↓
Inventory Reserved
↓
Reserve Credit
↓
Credit Approved
↓
Confirm Order
↓
Saga Completed

This slice proves that the complete business workflow can execute successfully.

Scope:
server/orchestrator-service only.

In scope:

1. Inbound handlers

Implement application handlers for:

- HandleOrderCreated
- HandleStockReserved
- HandleCreditApproved

These are application services.
Do NOT implement Kafka consumers yet.
The handlers receive domain/application events directly.

2. Outbound command ports

Create outbound ports:

- ReserveStockCommandPublisher
- ReserveCreditCommandPublisher
- ConfirmOrderCommandPublisher

These are application ports only.

Use Recording adapters for tests.

3. Saga memory

Extend Saga aggregate to remember:

- stockReservationId
- creditReservationId

These values are required for future compensation.

Do NOT add compensation logic yet.

4. Replace generic transitions

Replace generic:

    saga.advance(nextStep)

with semantic methods.

Preferred domain API:

- saga.inventoryReserved(stockReservationId)
- saga.creditApproved(creditReservationId)
- saga.complete()

The aggregate should express business language.

Do not expose advance() as the primary application API anymore.

5. Workflow

OrderCreated

- create saga
- persist
- publish SagaStarted
- publish ReserveStock command

StockReserved

- load saga
- saga.inventoryReserved(...)
- persist
- publish SagaAdvanced
- publish ReserveCredit command

CreditApproved

- load saga
- saga.creditApproved(...)
- persist
- publish SagaCompleted
- publish ConfirmOrder command

Do not introduce compensation.

Do not introduce retries.

Do not introduce timeout handling.

6. Test adapters

Create recording adapters for:

- ReserveStockCommandPublisher
- ReserveCreditCommandPublisher
- ConfirmOrderCommandPublisher

Use InMemorySagaRepository.

No Kafka.

No Avro.

No JPA.

Functional behavior

OrderCreated

Input:

- sagaId
- orderId
- customerId

Expected:

- Saga STARTED
- ORDER_CREATED
- ReserveStock command published

StockReserved

Input:

- sagaId
- stockReservationId

Expected:

- Saga remembers reservationId
- inventoryReserved()
- ReserveCredit command published

CreditApproved

Input:

- sagaId
- creditReservationId

Expected:

- Saga remembers reservationId
- creditApproved()
- complete()
- ConfirmOrder command published

Out of scope

- StockRejected
- StockPartiallyReserved
- CreditRejected
- Customer decision
- Backorder
- Compensation
- Timeout
- Retry
- Scheduler
- Dead Letter Queue
- Kafka consumers
- Kafka producers
- Avro mapper
- REST
- JPA
- Flyway
- Security
- RuntimeHints

Architecture rules

Keep the established Application Service grammar:

validate
↓

load/create aggregate
↓

execute semantic domain transition
↓

persist
↓

publish commands/events
↓

return result

The application service should read like the UC-01 story.

Avoid:

- large methods
- nested if/else
- duplicated state
- generic workflow language

Prefer semantic domain methods.

Logging

Use SLF4J.

Log:

- sagaId
- orderId
- stockReservationId
- creditReservationId
- currentStep
- currentStatus

Never log PII.

Transactionality

Do not introduce @Transactional.

Document:

Future JPA integration will introduce transaction boundaries.

Outbox remains the integration mechanism.

Native Image

Maintain compatibility.

No reflection.

No RuntimeHints.

Tests

Cover:

- complete happy path
- order created publishes ReserveStock
- stock reserved publishes ReserveCredit
- credit approved publishes ConfirmOrder
- saga stores reservation ids
- semantic methods invoked
- repository persistence
- event publication
- duplicate handlers where applicable
- validation failures

Architecture tests remain green.

Documentation

Create:

docs/implementation/ARB-015-saga-happy-path.md

Update:

- server/orchestrator-service/README.md
- docs/okf/index.md

Acceptance Criteria

- Entire happy path executes using in-memory adapters.
- No infrastructure required.
- Saga aggregate uses semantic transitions.
- Reservation IDs are persisted in Saga.
- Happy path fully tested.
- No compensation implemented.
- No Kafka runtime.
- No Avro runtime.
- No JPA.
- Ready for Deep review.

Important

This slice intentionally implements only the successful business path.

ARB-016 will introduce all failure paths:

- StockRejected
- StockPartiallyReserved
- CreditRejected
- Compensation
- Customer decision
- Backorder

Do not implement ARB-016.