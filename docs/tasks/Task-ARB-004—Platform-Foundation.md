Task: ARB-004 — Platform Foundation

Status:
[PLANNED]

Owner:
Clio

Context:
ARB-003 Architecture Skeleton is DONE after Deep review and cleanup.
Before creating the domain model, Arbitrier needs a small shared platform foundation for cross-cutting concerns.

Goal:
Implement minimal, domain-neutral platform primitives that all services can reuse.

Scope:
server/platform only, plus tests and documentation.

In scope:
1. Correlation primitives:
    - CorrelationId
    - CausationId
    - MessageId
    - RequestId
    - TraceId wrapper only if useful

2. Time abstraction:
    - TimeProvider or SystemClock abstraction
    - FixedTimeProvider for tests

3. Result/error primitives:
    - Result<T>
    - ProblemCode
    - ApplicationProblem / PlatformException if useful

4. Idempotency primitives:
    - IdempotencyKey
    - IdempotencyStatus
    - IdempotencyRecord
    - IdempotencyStore port interface only
    - No persistence implementation yet

5. Logging support:
    - SafeLoggable / SafeRenderable pattern
    - StructuredLogFields constants
    - no PII logging guideline

6. Observability support:
    - ObservationNames constants
    - AttributeNames constants
    - lightweight helpers only
    - no OpenTelemetry wiring yet unless already trivial and dependency-free

7. Validation support:
    - Require / Preconditions utility
    - String/UUID validation helpers if needed

8. Test support:
    - TestIds factory
    - FixedClock fixture
    - PlatformAssertions if useful

Out of scope:
- No business domain classes.
- No Order, Saga, Inventory, Credit, Money.
- No JPA.
- No Kafka.
- No Avro.
- No REST.
- No Spring Security.
- No OpenTelemetry runtime configuration.
- No database idempotency implementation.
- No outbox/inbox implementation yet.

Package structure:
server/platform/src/main/java/<base-package>/platform/
├── correlation/
├── time/
├── result/
├── error/
├── idempotency/
├── logging/
├── observability/
├── validation/
└── test/

Requirements:
- Every package must include package-info.java.
- Every public type must include Javadocs.
- Keep classes small and immutable.
- Prefer records/value objects where appropriate.
- Validate nulls and blank values.
- Avoid Lombok.
- Avoid Spring dependencies unless absolutely necessary.
- No external infra dependency should be required to run tests.

Tests:
- Unit tests for value object creation and validation.
- Unit tests for TimeProvider/FixedTimeProvider.
- Unit tests for Result success/failure.
- Unit tests for idempotency primitives.
- Unit tests for SafeLoggable/SafeRenderable behavior.
- Tests must run without Docker, Kafka, Postgres, or Keycloak.

Documentation:
- Create docs/implementation/ARB-004-platform-foundation.md.
- Update server/platform/README.md.
- Update docs/okf/index.md only if needed.
- Document what is intentionally not implemented yet.

Acceptance Criteria:
- server/platform compiles.
- Platform unit tests pass.
- No service module depends on new platform classes yet unless required for compile checks.
- No business domain concepts are introduced.
- No infrastructure implementations are introduced.
- package-info.java exists for all packages.
- Javadocs exist for all public types.
- Implementation note exists.
- ARB-004 remains ready for Deep review.

After completion:
- Report created files.
- Report tests run.
- Report open questions.
- Do not start ARB-005.