TASK: ARB-014-REVIEW
DATE: 2026-07-08
AGENT: clio

SUMMARY
-------
Deep review of ARB-014 Saga Orchestrator Foundation. All 52 tests pass
(19 SagaTest, 10 AdvanceSagaServiceTest, 9 StartSagaServiceTest, 9 CompensateSagaServiceTest,
4 ArchitectureTest, 1 OrchestratorServiceApplicationIT). The implementation
is clean, well-scoped, and establishes a solid hexagonal foundation for the
orchestrator. The `advance(SagaStep)` API is acceptable as a foundation
mechanic with the understanding that semantic transitions replace it in ARB-015.
No blockers. Verdict: PASS WITH WARNINGS.

FILES REVIEWED
--------------
~ docs/implementation/ARB-014-saga-orchestrator.md
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/domain/model/Saga.java
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/domain/model/SagaStatus.java
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/domain/model/SagaStep.java
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/domain/model/SagaId.java
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/domain/model/CompensationAction.java
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/domain/model/CustomerDecision.java
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/domain/event/SagaStartedDomainEvent.java
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/domain/event/SagaAdvancedDomainEvent.java
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/domain/event/SagaCompensatedDomainEvent.java
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/application/service/StartSagaService.java
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/application/service/AdvanceSagaService.java
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/application/service/CompensateSagaService.java
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/application/port/inbound/*.java (9 files)
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/application/port/outbound/*.java (2 files)
~ server/orchestrator-service/src/main/java/com/arbitrier/orchestrator/config/OrchestratorServiceConfiguration.java
~ server/orchestrator-service/src/test/java/com/arbitrier/orchestrator/domain/SagaTest.java
~ server/orchestrator-service/src/test/java/com/arbitrier/orchestrator/application/service/StartSagaServiceTest.java
~ server/orchestrator-service/src/test/java/com/arbitrier/orchestrator/application/service/AdvanceSagaServiceTest.java
~ server/orchestrator-service/src/test/java/com/arbitrier/orchestrator/application/service/CompensateSagaServiceTest.java
~ server/orchestrator-service/src/test/java/com/arbitrier/orchestrator/adapter/outbound/InMemorySagaRepository.java
~ server/orchestrator-service/src/test/java/com/arbitrier/orchestrator/adapter/outbound/RecordingSagaEventPublisher.java
~ server/orchestrator-service/src/test/java/com/arbitrier/orchestrator/unit/ArchitectureTest.java
~ server/orchestrator-service/src/test/java/com/arbitrier/orchestrator/integration/OrchestratorServiceTestConfiguration.java
~ server/orchestrator-service/src/test/java/com/arbitrier/orchestrator/integration/OrchestratorServiceApplicationIT.java

OBSERVATIONS
------------

Verdict: PASS WITH WARNINGS

Blockers: None.

Warnings:

1. Saga.advance(nextStep) accepts ANY SagaStep regardless of current state
   (domain/model/Saga.java:76-83). The domain validates only non-terminal and
   non-COMPENSATING but does not enforce step ordering. For example, calling
   advance(RESERVE_INVENTORY) followed by advance(ORDER_CREATED) would go
   backward without error. This is explicitly deferred to ARB-015 in the
   implementation doc (OPEN QUESTION 2), which is the right call — but it
   means the domain currently allows nonsensical transitions.

2. Saga.complete() sets step to COMPLETE_ORDER automatically (line 108),
   but Saga.cancel() preserves the current step rather than setting a
   CANCELLED step (line 120). This is a minor inconsistency in how terminal
   transitions handle the step field. The SagaStatus enum already distinguishes
   COMPLETED from CANCELLED, so the step field is sometimes redundant when
   status is terminal. Recommend harmonizing in ARB-015.

Saga Modeling Concerns:

-- Generic advance(nextStep) vs semantic transitions --

The current API exposes:
  saga.advance(SagaStep.RESERVE_INVENTORY)
  saga.advance(SagaStep.VALIDATE_CREDIT)

The preferred UC-01 semantic API would be:
  saga.inventoryReserved()
  saga.inventoryPartiallyReserved()
  saga.creditApproved()
  saga.creditRejected()
  saga.compensate()
  saga.complete()

Assessment:

A) Accept generic advance(nextStep) for ARB-014 foundation only.
   Recommendation: A. The generic advance is appropriate for the foundation
   slice — it proves the mechanics (load, validate invariants, transition,
   persist, publish) without committing to business-level step ordering.
   The domain already has semantic methods for special transitions:
   awaitCustomerDecision(), applyCustomerDecision(), compensate(),
   complete(), cancel(). The remaining ORDER_CREATED -> RESERVE_INVENTORY ->
   VALIDATE_CREDIT transitions are pure step changes that advance(nextStep)
   handles correctly.

B) Do NOT return to Clio. The generic approach is correct for this slice.
   ARB-015 should replace all remaining advance() calls with semantic
   methods once the step sequence is defined.

The semantic methods that should exist for UC-01:
  saga.inventoryReserved()           -> step RESERVE_INVENTORY (status STARTED)
  saga.inventoryPartiallyReserved()  -> step AWAIT_CUSTOMER_DECISION
  saga.creditApproved()              -> step VALIDATE_CREDIT
  saga.creditRejected()              -> triggers compensate() path
  saga.compensateInventory()         -> exists, step COMPENSATE_INVENTORY
  saga.compensateCredit()            -> exists, step COMPENSATE_CREDIT

-- Single generic workflow vs specific saga models --

Assessment: The current codebase has already started down the "common
infrastructure plus specific saga model" path. The Saga aggregate contains
UC-01-specific fields (orderId, customerId, customerDecision) and step
values (ORDER_CREATED, RESERVE_INVENTORY, VALIDATE_CREDIT, etc.). Pure
generic workflow infrastructure would not embed business step names.

Recommendation: Continue the current trajectory. Keep Saga as a
UC-01-specific aggregate with semantic methods. The SagaStatus enum
(COMPLETED, CANCELLED, FAILED_COMPENSATION) and SagaStep enum provide
enough structure for UC-01. If a UC-02 emerges later, introduce a
separate SagaUC02 aggregate or use a strategy/composition pattern.
Do not abstract to a generic workflow engine — it would add accidental
complexity before a second story exists.

Compensation Design:

- CompensateSagaService correctly only marks saga COMPENSATING and emits
  event. It does not issue compensation commands yet — correctly scoped.
- The decision that orchestrator decides WHEN to compensate but delegates
  WHAT to do to Inventory/Credit is correct. The orchestrator's role is
  saga lifecycle management, not execution of release commands.
- Saga memory needed for ARB-015/ARB-016 wiring:
  - stockReservationId (inventory response -> orchestrator memory)
  - creditReservationId (credit response -> orchestrator memory)
  - customerId (already on Saga)
  - flags: isInventoryReserved, isCreditApproved (implied by currentStep)
  The Saga aggregate currently has no field for storing external reservation
  IDs. These will be needed when the orchestrator must route compensation
  events to the correct service instance. Recommend adding optional
  stockReservationId and creditReservationId fields on the Saga in ARB-015
  when the happy path is wired.

- The Saga.compensateInventory() and Saga.compensateCredit() methods exist
  on the domain model already (lines 128, 139) — these are ready for
  ARB-016 wiring. Good forward planning.

- CompensationAction enum exists (RELEASE_INVENTORY_RESERVATION,
  RELEASE_CREDIT_RESERVATION, NONE) but is not used by any application
  service yet. This is acceptable for ARB-014 — it will be used in
  ARB-016 to route compensation decisions.

Multi-Story Orchestrator Design:

Recommendation: Keep the current trajectory:

- Saga aggregate stays specific to UC-01 (customerId, orderId,
  customerDecision, UC-01 steps).
- Do not add a generic Workflow/Step/Transition framework until a
  second business story (UC-02) materializes.
- When UC-02 appears, evaluate whether SagaUC02 is a new aggregate or
  whether Saga should be refactored to use composition (e.g., a generic
  Saga shell with a UC-01-specific state machine plugged in).
- The SagaStatus enum is already general-purpose (STARTED,
  AWAITING_CUSTOMER_DECISION, COMPENSATING, COMPLETED, CANCELLED,
  FAILED_COMPENSATION) — this could form the common infrastructure
  kernel.

Scope Discipline: PASS
- No Kafka producer/consumer (only package-info stubs).
- No Avro imports.
- No JPA entity.
- No REST controller.
- No happy-path wiring.
- No compensation-command execution.
- No timeout/retry/scheduler.

Layer Boundaries: PASS
- ArchitectureTest enforces 5 rules (identical to other services).
- Domain is pure Java — zero Spring/JPA/Kafka imports.
- Application depends only on domain types and platform library.
- Test adapters in test source tree, not in production code.
- OrchestratorServiceConfiguration returns port interfaces (StartSagaUseCase,
  AdvanceSagaUseCase, CompensateSagaUseCase) — correctly follows hexagonal
  convention (unlike credit-service which returns implementation classes).

Saga Lifecycle: PASS
- Start: Saga.start() creates saga at STARTED + ORDER_CREATED.
- Advance: saga.advance(nextStep) transitions step, preserves status,
  validates non-terminal and non-COMPENSATING.
- Compensate: saga.compensate() transitions to COMPENSATING, preserves
  step, validates non-terminal and not already COMPENSATING.
- Terminal enforcement: SagaStatus.isTerminal() covers COMPLETED,
  CANCELLED, FAILED_COMPENSATION. All transitions guard against terminal
  state. Dead sagas stay dead.
- COMPENSATING is correctly non-terminal (not in isTerminal() return set).

Events: PASS
- SagaStartedDomainEvent(sagaId, orderId, customerId) — published on start.
- SagaAdvancedDomainEvent(sagaId, orderId, currentStep) — published on advance.
- SagaCompensatedDomainEvent(sagaId, orderId) — published on compensate.
- All pure Java records — no Avro/Kafka in domain event classes.

Transactionality / Outbox: PASS
- @Transactional absent (no JPA adapter yet). Correct.
- Documentation clearly states deferral to JPA/Outbox phase.

Logging: PASS
- StartSagaService logs: sagaId, orderId, step.
- AdvanceSagaService logs: sagaId, orderId, step, status.
- CompensateSagaService logs: sagaId, orderId, step, status.
- All opaque IDs. No PII. No sensitive data.

Native Image Compatibility: PASS
- No reflection, dynamic proxies, Class.forName, or runtime scanning.
- No RuntimeHintsRegistrar needed.

Tests: PASS (52 tests, all green)
- SagaTest (19 tests): start fields, advance (normal, terminal, compensating,
  null step), compensate (normal, terminal, already compensating), complete,
  cancel, terminal guard, failCompensation, awaitCustomerDecision,
  applyCustomerDecision, CustomerDecision enum coverage.
- StartSagaServiceTest (9 tests): result, persist fields, event published,
  event exclusivity, validation (blank sagaId, orderId, customerId).
- AdvanceSagaServiceTest (10 tests): result, persist, status preserved,
  event published/fields/ exclusivity, terminal/compensating throws,
  not-found throws, validation (blank sagaId, null nextStep).
- CompensateSagaServiceTest (9 tests): result, persist, step preserved,
  event published/fields/exclusivity, already-compensating throws,
  terminal throws, not-found throws, validation.
- ArchitectureTest (4 tests): same 5 rules as other services.
- All tests unit-level without Spring, plus one @SpringBootTest IT.
- No Docker, Kafka, Postgres, Keycloak, or Schema Registry required.

Documentation: PASS
- Implementation note (ARB-014-saga-orchestrator.md) accurately reflects
  all behavior, open questions, and deferred responsibilities.
- Open questions are valid and not hiding blockers:
  1. Duplicate saga on start (deferred to Kafka consumer layer).
  2. Step validation / sequential ordering (deferred to ARB-015).
  3. Saga not found -> typed problem code (deferred to adapter).
  4. Transactionality + Outbox (deferred to JPA phase).
  5. COMPENSATING to CANCELLED/FAILED (deferred to ARB-016).
  6. customerId in compensation command (analyzed, solved by loading saga).
- ARB-015 boundary (happy path wiring) and ARB-016 boundary (compensation
  wiring) are clearly documented.

RECOMMENDATIONS
---------------

1. Accept advance(nextStep) as ARB-014 foundation only. Replace with
   semantic methods in ARB-015.

2. Introduce saga memory fields (stockReservationId, creditReservationId)
   in ARB-015 when the happy path must route responses back to the saga.

3. Harmonize complete() vs cancel() step field behavior in ARB-015:
   either both set a named step, or neither does (the status already
   distinguishes terminal states).

DECISION
--------
ARB-014 may be marked [DONE]. The advance(nextStep) API is acceptable
as a foundation mechanic. Semantic transitions, step ordering, saga
memory, and compensation wiring are correctly deferred to ARB-015 and
ARB-016. No return to Clio required.
