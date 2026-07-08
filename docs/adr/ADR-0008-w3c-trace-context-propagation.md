# ADR-0008 — W3C Trace Context as Primary Distributed Tracing Propagation Standard

| Field  | Value      |
|--------|------------|
| Status | Accepted   |
| Date   | 2026-07-07 |

## Intention

Establish W3C Trace Context (`traceparent` / `tracestate`) as the sole distributed tracing propagation standard for Arbitrier, and explicitly reject B3 propagation except as a future interoperability fallback.

## Context

Arbitrier uses OpenTelemetry SDK (no Java agent) with an OTLP exporter for distributed tracing across all saga steps, as documented in RNF-0001 and ADR-0007. OpenTelemetry supports multiple propagation formats:

| Format | Specification | Header names |
|--------|--------------|--------------|
| W3C Trace Context | W3C Recommendation (2021) | `traceparent`, `tracestate` |
| B3 (single-header) | Zipkin / OpenZipkin | `b3` |
| B3 (multi-header) | Zipkin / OpenZipkin | `X-B3-TraceId`, `X-B3-SpanId`, `X-B3-Sampled` |

OpenTelemetry's Java SDK and Spring Boot's Micrometer Tracing integration both default to W3C Trace Context. B3 is a legacy format from Zipkin that predates the W3C standard.

ARB-008 raised an open question: "Should the filter also support `X-B3-TraceId` for OpenTelemetry trace correlation before the OTLP SDK is wired?" This ADR answers that question.

## Decision

**W3C Trace Context (`traceparent` / `tracestate`) is the primary and default distributed tracing propagation format for all Arbitrier services.**

B3 propagation (`X-B3-TraceId` / `X-B3-SpanId` / `X-B3-Sampled`) must not be added to service code or platform infrastructure unless a specific, documented integration with a legacy system requires it. Any such addition requires an ADR update.

Practical consequences:

- OpenTelemetry Java SDK must be configured with `W3CTraceContextPropagator` (the SDK default) — no explicit change needed unless configuration overrides it.
- Spring Boot Micrometer Tracing bridge must not be configured with `B3Propagator`.
- The `CorrelationFilter` in `platform/web` propagates application-level `X-Correlation-Id` / `X-Request-Id` headers. These are **not** trace context headers and must not be confused with `traceparent`. Both coexist without conflict.
- Kafka message headers, when OTel Kafka instrumentation is wired, will carry `traceparent` / `tracestate` — not B3 headers.
- Downstream services integrated over HTTP must also honor `traceparent` / `tracestate`. If a legacy service only understands B3, document the interoperability gap as an `OPEN QUESTION` at the integration boundary.

## Consequences

- W3C Trace Context is an IETF/W3C recommendation: all modern observability backends (Jaeger, Tempo, Datadog, Cloud Trace) support it natively.
- No dual-propagation configuration required, reducing complexity.
- If a future B3-only legacy integration is unavoidable, OpenTelemetry's `CompositeTextMapPropagator` can support both formats simultaneously — that choice would require an ADR update at the time.

## Open Questions

- OPEN QUESTION: Exact OTLP endpoint and sampling strategy (head-based vs tail-based) for the deployment target — deferred to the observability wiring task.
- OPEN QUESTION: Whether the Kafka OTel instrumentation bridge (`opentelemetry-instrumentation-kafka`) requires native hints when the first Kafka producer/consumer is introduced (see ADR-0007).
