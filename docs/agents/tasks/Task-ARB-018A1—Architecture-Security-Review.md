Task: ARB-018A1 — Architecture Security Review

Status:
[PLANNED]

Owner:
Deep

Context:

Arbitrier has completed its foundational architecture.

Current completed work includes:

- Domain model
- Bounded contexts
- Hexagonal architecture
- Application services
- Saga orchestration
- Retry policy
- JPA persistence adapters
- Flyway migrations
- PostgreSQL schemas
- Runtime local stack
- Customer Portal prototype
- Documentation audit

The project is approaching the integration phase:

- Kafka adapters
- Outbox
- REST APIs
- Keycloak integration
- Dashboard
- Observability

This is the ideal moment to perform an architectural security review before external
integration begins.

This is NOT a penetration test.

This is NOT an implementation task.

This is a design review.

--------------------------------------------------

Review Goal

Evaluate whether the current architecture provides appropriate security boundaries
and identify architectural risks before the integration phase begins.

Focus on:

- trust boundaries
- attack surface
- ownership
- isolation
- future evolution

Do not review coding style.

Do not propose speculative redesigns.

Recommendations should be proportional and actionable.

--------------------------------------------------

1. Domain boundaries

Review:

- bounded contexts
- aggregate ownership
- domain invariants
- identifier exposure
- ownership leakage

Questions:

Does any bounded context expose internal implementation details?

Are there domain concepts that should remain private?

Are identifiers unnecessarily exposed?

--------------------------------------------------

2. Authentication architecture

Review planned Keycloak integration.

Evaluate:

- issuer validation
- audience validation
- JWT trust model
- token lifetime assumptions
- refresh token strategy
- service-to-service authentication

Identify missing architectural decisions.

--------------------------------------------------

3. Authorization model

Review:

Customer

Company

Operations

Administrator

Questions:

Can one customer accidentally affect another?

Is tenant isolation clearly defined?

Are future authorization responsibilities located in the appropriate services?

--------------------------------------------------

4. Saga security

Review:

Saga lifecycle

Commands

Events

Compensation

Timeouts

Retry

Questions:

Could an attacker trigger compensation?

Could duplicate events alter saga state?

Are state transitions sufficiently protected?

Could replay attacks become an issue?

Evaluate the architecture rather than implementation details.

--------------------------------------------------

5. Event architecture

Review Kafka event model.

Evaluate:

- command authenticity
- event authenticity
- replay resistance
- duplicate handling
- ordering assumptions
- trust boundaries between producers and consumers

Identify architectural protections that should exist before production.

--------------------------------------------------

6. Persistence

Review:

JPA

Flyway

Optimistic locking

Schema ownership

Questions:

Could one service accidentally mutate another service's data?

Are transaction boundaries appropriate?

Does optimistic locking sufficiently protect concurrent updates?

--------------------------------------------------

7. REST surface (future)

Evaluate the planned REST architecture.

Review:

validation

mass assignment

idempotency

pagination abuse

request size

error exposure

problem details

Suggest architectural protections.

--------------------------------------------------

8. Runtime & Infrastructure

Review:

Docker Compose

PostgreSQL

Kafka

Schema Registry

Keycloak

Questions:

Are secrets isolated?

Are default credentials acceptable for local-only runtime?

Are exposed ports appropriate?

Does the runtime accidentally expose production assumptions?

--------------------------------------------------

9. Logging & Observability

Review:

SafeLoggable

ProblemCode

Correlation IDs

Questions:

Could sensitive business information appear in logs?

Could customer identifiers leak?

Are future audit requirements supported?

Evaluate logging architecture.

--------------------------------------------------

10. Supply chain

Review:

dependencies

test dependencies

plugin usage

Flyway

Spring

Kafka

Testcontainers

Highlight obvious architectural risks.

No CVE scanning required.

--------------------------------------------------

11. Native Image readiness

Review the architecture with future GraalVM support in mind.

Do not request changes solely for native compatibility.

Only identify architectural choices likely to become blockers.

--------------------------------------------------

12. Documentation

Verify security assumptions are documented where appropriate.

Review:

README

ADRs

RNFs

Implementation reports

Architecture documentation

Identify undocumented security assumptions.

--------------------------------------------------

Output

Classify findings using:

PASS

PASS WITH WARNINGS

FAIL

Each finding should include:

Severity:

- INFO
- LOW
- MEDIUM
- HIGH
- CRITICAL

Category:

- Architecture
- Authentication
- Authorization
- Domain
- Events
- Persistence
- Runtime
- Logging
- Documentation
- Future Evolution

For every warning:

Explain:

- why it matters
- impact
- recommended mitigation
- whether it blocks the roadmap

--------------------------------------------------

Important

This review should prioritize architectural correctness.

Avoid recommending implementation work that belongs to future slices.

Respect decisions already captured in ADRs unless a significant security concern exists.

--------------------------------------------------

Expected outcome

The goal is to answer:

"Can we confidently begin the Kafka/REST integration phase, or are there architectural security issues that should be resolved first?"