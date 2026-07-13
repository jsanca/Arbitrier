Task: ARB-016 — Saga Compensation

Status:
[COMPLETE]

Owner:
Clio

Context:
ARB-015 Saga Happy Path is DONE.
The orchestrator can now process:
OrderCreated → StockReserved → CreditApproved → ConfirmOrder → SagaCompleted.

ARB-016 introduces failure paths and compensation.

Goal:
Implement saga failure handling for UC-01:
- StockRejected
- CreditRejected
- compensation start
- ReleaseStock command
- ReleaseCredit command if needed
- Saga cancellation / failed compensation state

Scope:
server/orchestrator-service only.

In scope:

1. Inbound handlers:
- HandleStockRejectedUseCase
- HandleCreditRejectedUseCase
- HandleStockReleasedUseCase
- HandleCreditReleasedUseCase
- HandleCompensationFailedUseCase if needed

2. Commands/results:
- HandleStockRejectedCommand/Result
- HandleCreditRejectedCommand/Result
- HandleStockReleasedCommand/Result
- HandleCreditReleasedCommand/Result

3. Outbound compensation command ports:
- ReleaseStockCommandPublisher
- ReleaseCreditCommandPublisher

4. Saga semantic methods:
   Prefer business methods over generic advance:
- stockRejected()
- creditRejected()
- startCompensation()
- inventoryReleased()
- creditReleased()
- cancel()
- failCompensation()

5. Compensation behavior:

StockRejected:
- load saga
- mark stock rejected
- cancel saga
- publish SagaCancelled
- do NOT release stock
- do NOT release credit

CreditRejected after StockReserved:
- load saga
- mark credit rejected
- transition saga to COMPENSATING
- publish ReleaseStock command using stored stockReservationId
- do NOT release credit because credit was never approved

StockReleased after compensation:
- load saga
- mark inventory compensation complete
- cancel saga
- publish SagaCancelled

CreditReleased:
- may be needed later for flows where credit was approved and later failure occurs
- implement only if current model supports it cleanly
- otherwise document as deferred

CompensationFailed:
- load saga
- transition to FAILED_COMPENSATION
- publish failure event

Out of scope:
- No partial stock path.
- No customer decision.
- No backorder.
- No Kafka consumers/producers.
- No Avro mappers.
- No JPA.
- No REST.
- No timeout/retry scheduler.
- No DLQ.
- No production adapters.

Architecture:
- Domain remains pure Java.
- Application does not depend on Kafka/JPA/Avro/REST.
- Command publishers are outbound ports.
- Domain events are pure Java.
- Saga owns transition invariants.
- Application services read as business stories.

Tests:
- StockRejected cancels saga.
- StockRejected emits no release commands.
- CreditRejected after stock reserved starts compensation.
- CreditRejected emits ReleaseStock command.
- StockReleased after compensation cancels saga.
- Compensation failure moves saga to FAILED_COMPENSATION.
- Cannot compensate without required reservation IDs.
- Terminal sagas cannot be modified.
- No duplicate release command on repeated compensation event if modeled.
- Architecture tests remain green.

Documentation:
Create:
- docs/implementation/ARB-016-saga-compensation.md

Update:
- server/orchestrator-service/README.md
- docs/okf/index.md
- relevant TC files for credit rejected and compensation

Acceptance Criteria:
- orchestrator-service compiles.
- tests pass without Kafka, Postgres, Keycloak, Schema Registry, Docker.
- compensation paths are implemented using in-memory adapters.
- no infrastructure introduced.
- failure paths are semantically modeled.
- ARB-016 is ready for Deep review.

Important:
ARB-016 handles compensation only.
Do not implement partial/backorder/customer-decision flow.
That belongs to ARB-017.
