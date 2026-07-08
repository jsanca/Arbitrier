Task: ARB-008 — Shared Platform Spring Integration

Status:
[PLANNED]

Owner:
Clio

Context:
ARB-004 created pure platform primitives.
ARB-007 created the first order-service application slice.
Now we need shared Spring-friendly platform infrastructure that services can reuse without contaminating domain models.

Goal:
Create reusable platform support for service modules:
- correlation propagation
- request logging
- error response mapping
- time provider bean
- basic validation/error conventions
- test utilities for service slices

Module in scope:
- server/platform

Allowed service updates:
- order-service only if needed to consume the new platform configuration.
- Do not modify inventory, credit, or orchestrator unless required for compile consistency.

In scope:
1. Correlation support
    - CorrelationId header convention
    - RequestId generation
    - MDC population
    - servlet filter or interceptor
    - constants for headers

2. Error handling support
    - Problem response DTO
    - Platform exception mapping
    - validation error mapping
    - reusable ControllerAdvice if appropriate

3. Time provider Spring wiring
    - SystemClock bean configuration
    - no domain dependency

4. Logging support
    - MDC fields:
        - correlationId
        - requestId
        - messageId where applicable later
    - no raw PII logging

5. Test support
    - helper for controller tests
    - correlation header assertions if useful

6. Documentation
    - docs/implementation/ARB-008-shared-platform.md
    - update server/platform/README.md
    - update AGENTS.md / CLAUDE.md only if needed

Out of scope:
- No Keycloak.
- No Spring Security.
- No customer authorization.
- No Kafka.
- No Avro mappers.
- No JPA.
- No Outbox/Inbox.
- No domain model changes.
- No business use cases.
- No gateway/BFF.

Important architectural rule:
Platform may provide Spring integration, but domain and application layers must remain framework-independent.

Package proposal:
server/platform/src/main/java/com/arbitrier/platform/
├── web/
│   ├── CorrelationHeaders
│   ├── CorrelationFilter
│   ├── ProblemResponse
│   └── PlatformExceptionHandler
├── spring/
│   └── PlatformAutoConfiguration or PlatformConfiguration
└── logging/
└── MdcKeys or reuse StructuredLogFields

Native Image:
- Keep Spring AOT compatibility.
- Avoid reflection-heavy patterns.
- If ControllerAdvice/filter requires hints, document OPEN QUESTION.
- No RuntimeHints unless necessary.

Tests:
- Correlation filter adds/generates correlationId.
- Existing correlationId is preserved.
- requestId is generated per request.
- MDC is cleared after request.
- ProblemResponse maps ApplicationProblem.
- Validation errors map to safe response.
- Tests run without Docker, Kafka, Postgres, Keycloak, or Schema Registry.

Acceptance Criteria:
- platform compiles.
- platform tests pass.
- order-service still compiles/tests pass if touched.
- No business logic introduced.
- No security implementation introduced.
- Correlation/logging/error conventions documented.
- ARB-008 ready for Deep review.

After completion:
- Report created files.
- Report tests run.
- Report open questions.
- Do not start ARB-009.