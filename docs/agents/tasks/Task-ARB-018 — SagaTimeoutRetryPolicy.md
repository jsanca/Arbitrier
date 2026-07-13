Task: ARB-018 — Saga Timeout & Retry Policy

Status:
[COMPLETE]

Owner:
Clio

Context

ARB-017B completed the inventory redesign.

The Saga now models:

- happy path
- compensation
- pre-saga negotiation
- global inventory allocation

The remaining missing behavior is handling sagas that never receive an expected response.

This slice models timeout behavior as a business concern.

Do NOT introduce runtime scheduling or infrastructure.

Goal

Model timeout and retry policy inside the Saga domain and application.

The implementation defines:

- timeout states
- timeout transitions
- retry decisions
- timeout domain events

Actual scheduling and retry execution will be implemented later during Runtime / Infrastructure slices.

Scope

Primary module:

server/orchestrator-service

Documentation updates only elsewhere.

--------------------------------------------------

1. Domain model

Extend Saga semantics.

Introduce explicit timeout transitions.

Suggested semantic methods:

inventoryTimedOut()

creditTimedOut()

retryInventory()

retryCredit()

failTimeout()

Choose names that best express business intent.

Avoid generic methods such as:

timeout(step)

advance()

--------------------------------------------------

2. Saga state

Review whether new states are required.

Possible examples:

WAITING_FOR_INVENTORY

WAITING_FOR_CREDIT

TIMED_OUT

FAILED_TIMEOUT

Do not add states unless they improve domain clarity.

Prefer explicit transitions over status flags.

--------------------------------------------------

3. Retry policy model

Create a pure Java policy model.

Suggested concepts:

RetryDecision

RetryPolicy

RetryOutcome

RetryContext

The policy should answer:

retry?

delay?

terminal failure?

Do NOT sleep, schedule or execute retries.

Only model the decision.

--------------------------------------------------

4. Timeout events

Introduce pure domain events such as:

InventoryTimedOutDomainEvent

CreditTimedOutDomainEvent

SagaTimedOutDomainEvent

RetryScheduledDomainEvent (optional)

Keep events transport agnostic.

--------------------------------------------------

5. Application services

Introduce inbound use cases such as:

HandleInventoryTimeoutService

HandleCreditTimeoutService

These services:

- load saga
- execute semantic transition
- persist
- publish domain events
- return application result

They must read as business stories.

--------------------------------------------------

6. Timeout policy

Represent timeout durations as domain/application configuration.

Do not depend on Spring configuration.

A simple value object is sufficient.

Example:

TimeoutPolicy

InventoryTimeout

CreditTimeout

The implementation should remain framework independent.

--------------------------------------------------

7. Out of scope

No Kafka retry topics.

No DLQ.

No scheduler.

No @Scheduled.

No Quartz.

No Spring Retry.

No Resilience4j.

No persistence changes.

No Docker.

No Kubernetes.

No Terraform.

No OpenTelemetry.

--------------------------------------------------

8. Architecture

Domain remains pure Java.

Application depends only on ports/domain.

Ports remain transport agnostic.

No runtime infrastructure.

--------------------------------------------------

9. Tests

Cover:

Inventory timeout

Credit timeout

Terminal timeout

Retry decision

No retry after terminal failure

Duplicate timeout

Timeout after completed saga

Architecture tests remain green.

--------------------------------------------------

10. Documentation

Create:

docs/implementation/ARB-018-saga-timeout-policy.md

Update:

Relevant RF

Relevant OKF

README if necessary.

Document clearly:

Business timeout != runtime scheduler.

--------------------------------------------------

Acceptance Criteria

- orchestrator tests pass.
- timeout modeled entirely in pure Java.
- no infrastructure introduced.
- retry policy expressed through domain/application model.
- ready for runtime implementation in future infrastructure slices.

After completion

Report:

- created/modified files
- tests
- open questions

Do not start the next roadmap task.
