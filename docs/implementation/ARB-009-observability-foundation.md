# ARB-009 — Observability Foundation

| Field  | Value       |
|--------|-------------|
| Task   | ARB-009     |
| Status | Implemented |
| Date   | 2026-07-07  |

## Changes Made

### New: `platform/web/TraceContextHeaders`

Constants for W3C Trace Context header names:

| Constant | Value | Purpose |
|----------|-------|---------|
| `TRACEPARENT` | `"traceparent"` | Trace ID + parent span ID + sampling flags |
| `TRACESTATE` | `"tracestate"` | Vendor-specific trace state; forwarded unchanged |

These constants are owned by the OpenTelemetry SDK. Application code must reference `TraceContextHeaders` when asserting or logging about trace context headers, but must never generate or overwrite these headers outside OTel instrumentation.

### Enhanced: `platform/logging/StructuredLogFields` Javadoc

Class-level Javadoc now documents which MDC keys are populated by which component:

| MDC key | Populated by | Status |
|---------|-------------|--------|
| `correlationId` | `CorrelationFilter` | Active |
| `requestId` | `CorrelationFilter` | Active |
| `traceId` | OTel MDC bridge | Deferred |
| `spanId` | OTel MDC bridge | Deferred |
| `sagaId`, `orderId` | Application/adapter code | Added at saga-step boundaries |
| `messageId`, `causationId` | Kafka inbound adapters | Deferred to Kafka phase |

`traceId` and `spanId` reflect W3C Trace Context and are never populated from B3 headers.

### Updated: `order-service/application.yml`

Added `metrics` to the actuator endpoint exposure list:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

Prometheus export configuration (scrape path, port, labels) is deferred to the infrastructure wiring phase.

### Updated: `server/platform/README.md`

Added "Observability Conventions" section documenting the header taxonomy, MDC population sources, and actuator endpoints.

### Updated: `CLAUDE.md` and `AGENTS.md`

Added W3C / B3 guardrails to both agent instruction files so future implementation tasks do not introduce B3 by mistake.

## Observability Concept Map

```
HTTP request arrives
│
├── traceparent: 00-<traceId>-<parentSpanId>-01  ← OTel SDK reads this
│   tracestate: vendor=value                      ← OTel SDK reads this
│   X-Correlation-Id: <businessCorrelationId>     ← CorrelationFilter reads/generates
│
├─► CorrelationFilter (HIGHEST_PRECEDENCE)
│     MDC.put("correlationId", ...)
│     MDC.put("requestId",     ...)
│     [does NOT touch traceparent / tracestate]
│
├─► OTel SDK (deferred — not yet wired)
│     MDC.put("traceId", ...)    ← from traceparent trace-id
│     MDC.put("spanId",  ...)    ← current span
│
└─► Service code
      MDC.put("sagaId",  ...)    ← at saga-step entry
      MDC.put("orderId", ...)    ← at saga-step entry
```

Every log line at a saga step must include: `sagaId`, `orderId`, `traceId`, `correlationId`.

## Tests Added

### `CorrelationFilterTest` (4 new tests)

| Test | What it proves |
|------|----------------|
| `filter_does_not_generate_traceparent_header` | Filter never writes `traceparent`/`tracestate` to response |
| `filter_does_not_echo_or_overwrite_incoming_traceparent` | Filter ignores incoming `traceparent`/`tracestate` |
| `correlation_headers_work_alongside_traceparent` | Both header kinds coexist without conflict |
| `mdc_does_not_contain_trace_context_keys_set_by_filter` | `traceId` MDC key is not populated by the filter |

Total `CorrelationFilterTest` test count: 12 (8 existing + 4 new).

## What Was Intentionally Not Implemented

| Area | Reason |
|------|--------|
| `micrometer-tracing-bridge-otel` dependency | No tests need it yet; exporter wiring deferred |
| OTel SDK configuration / `application.yml` properties | No OTLP collector available in test environment |
| Prometheus scrape configuration | Infrastructure wiring phase |
| Kafka trace context propagation | Kafka adapters deferred |
| JPA trace instrumentation | JPA deferred |

## Open Questions

- OPEN QUESTION: When the OTel MDC bridge is wired, confirm that `traceId` in MDC reflects the W3C `traceparent` trace-id (128-bit hex) rather than a Zipkin-format trace ID.
- OPEN QUESTION: Should `micrometer-tracing-bridge-otel` be declared as an `optional` dependency in `platform/pom.xml` to make it easy for services to activate it, or should each service own that dependency explicitly?
- OPEN QUESTION: Prometheus scrape configuration — port, path, labels, and whether each service exposes its own endpoint or a sidecar aggregates (deferred to infrastructure phase).
