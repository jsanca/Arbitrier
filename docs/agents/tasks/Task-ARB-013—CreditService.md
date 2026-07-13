Task: ARB-013 — Credit Service

Status:
[COMPLETE]

Owner:
Clio

Context:
ARB-012 Inventory Service is DONE.
Credit Service is a saga participant.
It does not orchestrate orders.
It validates and reserves/consumes corporate credit for a customer, or rejects the request.

Goal:
Implement the first credit-service application slice:
- Reserve/approve credit for an order.
- Reject insufficient credit.
- Release approved credit idempotently.

Scope:
server/credit-service only.

In scope:
1. Inbound application ports:
- ReserveCreditUseCase
- ReleaseCreditUseCase

2. Application commands:
- ReserveCreditCommand
- ReleaseCreditCommand

3. Application results:
- ReserveCreditResult
- ReleaseCreditResult

4. Application services:
- ReserveCreditService
- ReleaseCreditService

5. Outbound ports:
- CreditLimitPort
- CreditReservationRepository
- CreditReservationEventPublisher

6. Domain events:
- CreditApprovedDomainEvent
- CreditRejectedDomainEvent
- CreditReleasedDomainEvent

7. Test adapters:
- ConfigurableCreditLimitPort
- InMemoryCreditReservationRepository
- RecordingCreditReservationEventPublisher

Functional behavior:

ReserveCreditUseCase:
- Input:
    - orderId
    - creditReservationId
    - customerId
    - amount

Validate:
- orderId required
- creditReservationId required
- customerId required
- amount required
- amount must be non-negative

Approved:
- If customer has enough available credit:
    - create CreditReservation with APPROVED status
    - persist through CreditReservationRepository
    - publish CreditApprovedDomainEvent
    - return APPROVED

Rejected:
- If customer does not have enough available credit:
    - create CreditReservation with REJECTED status
    - persist if consistent with current domain model
    - publish CreditRejectedDomainEvent
    - return REJECTED

ReleaseCreditUseCase:
- Load reservation by creditReservationId.
- If reservation is APPROVED:
    - call domain release()
    - persist RELEASED reservation
    - publish CreditReleasedDomainEvent
    - return RELEASED
- If reservation is already RELEASED:
    - return RELEASED without duplicate event
- If reservation is REJECTED:
    - no-op release unless current domain model rejects it
    - document and test chosen behavior

Important idempotency rule:
ReleaseCredit must be idempotent.

Out of scope:
- No Kafka consumer.
- No Kafka producer.
- No Avro mapper.
- No JPA entity.
- No Postgres repository.
- No Flyway/Liquibase.
- No REST controller unless strictly needed.
- No saga orchestration.
- No inventory logic.
- No order confirmation logic.
- No Keycloak/Spring Security integration.
- No native RuntimeHints.
- No real credit bureau/external integration.

Architecture rules:
- Domain remains pure Java.
- Application does not depend on REST/JPA/Kafka/Avro.
- Domain events must be pure Java.
- No generated Avro classes in application/domain.
- Application services should read as business pipelines:
  validate/derive → create domain result → save → publish → return.
- Avoid duplicated outcome state; derive result from the aggregate status.

Logging:
- Use SLF4J in application services.
- Log only opaque IDs, amount, currency, and status.
- Do not log credit limits or sensitive financial details unless explicitly safe.

Transactionality:
- Do not add @Transactional yet.
- Document that transaction boundary is deferred to JPA phase.
- DB + Kafka consistency will be handled by Outbox, not direct Kafka send inside DB transaction.

Native Image:
- Avoid reflection, Class.forName, dynamic proxies, runtime scanning.
- No RuntimeHints yet.

Tests:
- approve credit happy path
- reject insufficient credit
- missing orderId
- missing creditReservationId
- missing customerId
- invalid amount
- release approved reservation
- release is idempotent
- release rejected reservation behavior documented and tested
- unknown reservation behavior documented and tested
- repository save called
- correct domain event published for approve/reject/release
- no event published for idempotent release unless intentionally documented

Documentation:
Create:
- docs/implementation/ARB-013-credit-service.md

Update if needed:
- server/credit-service/README.md
- docs/okf/index.md
- docs/test-cases/TC-UC-01-002-happy-path-full-confirmation.md
- docs/test-cases/TC-UC-01-007-credit-rejected-compensation.md
- docs/test-cases/TC-UC-01-009-credit-timeout-after-stock-reserved.md

Acceptance Criteria:
- credit-service compiles.
- tests pass without Docker, Kafka, Postgres, Keycloak, Schema Registry, or native image build.
- ReserveCreditUseCase exists.
- ReleaseCreditUseCase exists.
- CreditLimitPort exists.
- CreditReservationRepository exists.
- CreditReservationEventPublisher exists.
- Domain events are pure Java.
- No Kafka runtime code.
- No Avro generated class usage.
- No JPA.
- No database migrations.
- ReleaseCredit behavior is explicitly idempotent.
- Documentation captures reserve/release decisions and open questions.
- ARB-013 is ready for Deep review.

After completion:
- Report created files.
- Report tests run.
- Report open questions.
- Do not start ARB-014.
