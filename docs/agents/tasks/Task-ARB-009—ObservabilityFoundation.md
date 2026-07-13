Task: ARB-009 — Observability Foundation

Status:
[COMPLETE]

Owner:
Clio

Context:
ARB-008 introduced shared web platform support.
ADR-0008 defines W3C Trace Context as the primary propagation standard.
B3 is allowed only for documented legacy interoperability.
ARB-009 must wire observability foundations without adding business behavior.

Goal:
Add baseline observability support across services:
- structured logs
- correlation/request IDs
- trace context propagation conventions
- OpenTelemetry-ready configuration
- actuator exposure
- documentation and tests

Modules in scope:
- server/platform
- order-service only as first consumer/demo if needed

In scope:
1. Platform observability conventions
    - constants for W3C headers:
        - traceparent
        - tracestate
    - confirm existing X-Correlation-Id / X-Request-Id remain application-level headers
    - document relationship:
        - traceId/spanId = technical trace
        - correlationId = business operation traceability
        - requestId = single HTTP request

2. Logging/MDC
    - ensure correlationId and requestId are placed in MDC
    - document expected future MDC keys:
        - traceId
        - spanId
        - sagaId
        - orderId
        - messageId
        - causationId
    - do not log PII

3. OpenTelemetry dependency/config placeholder
    - add only minimal dependencies/config if needed
    - avoid full exporter wiring unless simple and local-safe
    - no external collector required for tests

4. Actuator
    - verify health/info exposure remains stable
    - optionally expose metrics endpoint if dependency already exists

5. Tests
    - verify W3C headers are not overwritten by CorrelationFilter
    - verify correlation headers still work
    - verify MDC cleanup
    - verify no B3 constants/headers are introduced as first-class platform convention

6. Documentation
    - create docs/implementation/ARB-009-observability-foundation.md
    - update server/platform/README.md
    - update AGENTS.md / CLAUDE.md if agent guardrails need observability language
    - update docs/okf/index.md if needed

Out of scope:
- No Grafana.
- No Tempo.
- No Prometheus deployment.
- No OTLP collector.
- No Kubernetes observability manifests.
- No Kafka tracing yet.
- No JPA tracing yet.
- No dashboard.
- No business events.
- No inventory/credit/orchestrator implementation.
- No security integration.
- No B3 support unless documented as deferred legacy interoperability.

Important decisions:
- W3C Trace Context is the primary distributed tracing standard.
- B3 is not implemented.
- X-Correlation-Id is not a replacement for traceparent.
- CorrelationId is business-level traceability.
- traceparent/tracestate are technical distributed tracing context.

Native Image:
- Keep Spring AOT compatibility.
- Avoid reflection-heavy observability configuration.
- If OpenTelemetry instrumentation requires native hints, document OPEN QUESTION.
- Do not introduce native-hostile libraries without ADR update.

Acceptance Criteria:
- platform compiles.
- order-service compiles if touched.
- tests pass without Docker, Kafka, Postgres, Keycloak, OTLP collector, or Schema Registry.
- W3C Trace Context decision is reflected in code/docs.
- B3 is not introduced as a default.
- Correlation/request ID behavior remains intact.
- Documentation clearly explains correlationId vs traceId vs requestId.
- ARB-009 is ready for Deep review.

After completion:
- Report created files.
- Report tests run.
- Report open questions.
- Do not start ARB-010.
