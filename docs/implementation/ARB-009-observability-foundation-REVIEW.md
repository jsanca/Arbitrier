# ARB-009 — Observability Foundation — Review

| Field    | Value       |
|----------|-------------|
| Task     | ARB-009     |
| Status   | Implemented |
| Date     | 2026-07-07  |
| Reviewer | Deep        |

## Verdict

**PASS**

---

## Summary

ARB-009 correctly establishes the W3C Trace Context observability foundation. The W3C-only guarantee is enforced through three layers: code (`CorrelationFilter` never touches `traceparent`/`tracestate`), tests (4 dedicated tests verify this), and documentation (`AGENTS.md`, `CLAUDE.md`, `platform/README.md` all carry W3C-only guardrails). No premature wiring of OTel exporters, Kafka tracing, JPA instrumentation, or business telemetry. No scope leaks.

---

## 1. W3C Trace Context: PASS

| Criterion | Evidence |
|-----------|----------|
| `traceparent`/`tracestate` constants exist | `TraceContextHeaders.java` — `TRACEPARENT = "traceparent"`, `TRACESTATE = "tracestate"` |
| B3 not introduced as default | ADR-0008 rejects B3; `TraceContextHeaders` javadoc explicitly forbids B3; no B3 constants anywhere in codebase |
| `CorrelationFilter` does not touch W3C headers | Filter reads/writes only `X-Correlation-Id`/`X-Request-Id`. Never reads `traceparent`/`tracestate`. 4 dedicated tests verify no generation, no echo, coexistence, no MDC pollution |
| Docs distinguish `traceId`/`spanId` from `correlationId`/`requestId` | `TraceContextHeaders` javadoc table · `StructuredLogFields` population table · Implementation note concept map · `AGENTS.md` header taxonomy · `CLAUDE.md` · `platform/README.md` — all consistent |

---

## 2. MDC Behavior: PASS

| Criterion | Evidence |
|-----------|----------|
| `correlationId` and `requestId` placed in MDC | `CorrelationFilter.java:51-52` — `MDC.put(CORRELATION_ID, ...)`, `MDC.put(REQUEST_ID, ...)` |
| MDC cleared after request | `CorrelationFilter.java:59-61` — `finally` block calls `MDC.remove()` for both. Tests confirm cleanup on success (`mdc_is_cleared_after_successful_request`) and on throw (`mdc_is_cleared_even_when_chain_throws`) |
| `traceId`/`spanId` not populated by filter | `mdc_does_not_contain_trace_context_keys_set_by_filter` asserts `MDC.get(TRACE_ID)` is `null` during chain execution |
| `sagaId`/`orderId`/`messageId`/`causationId` documented as future boundary-specific fields | `StructuredLogFields.java` javadoc table clearly lists each key, its populator, and status (active/deferred). Implementation note concept map shows sagaId/orderId at saga-step boundaries |

---

## 3. Scope Discipline: PASS

| Concern | Status |
|---------|--------|
| No OTLP collector config | Not present; deferred to infrastructure phase |
| No Grafana/Tempo/Prometheus infra | Not present |
| No Kafka tracing | Listed as deferred; no Kafka dependencies |
| No JPA tracing | Listed as deferred; no JPA dependencies |
| No security integration | Not present |
| No business behavior changes | `CorrelationFilter` existed with 8 tests; 4 new tests are structural (W3C assertions). Only new file: `TraceContextHeaders.java` (constant class). `AttributeNames`/`ObservationNames` are pre-existing (ARB-004). |

---

## 4. Actuator: PASS

`order-service/application.yml` updated from `include: health,info` to `include: health,info,metrics`. No Prometheus export configuration (`management.metrics.export.prometheus.*`). This is the correct minimal change for this slice.

---

## 5. Native Image Compatibility: PASS

| Risk | Status |
|------|--------|
| Reflection-heavy instrumentation | None — constants + standard servlet filter + standard `@RestControllerAdvice` |
| Native-hostile libraries | No new dependencies; `opentelemetry-api` not yet added |
| `RuntimeHints` required | Not yet. OPEN QUESTION documents that `micrometer-tracing-bridge-otel` will need native hints when introduced |

---

## 6. Tests: PASS

| Criterion | Evidence |
|-----------|----------|
| W3C headers ignored by `CorrelationFilter` | 4 new tests: `filter_does_not_generate_traceparent_header`, `filter_does_not_echo_or_overwrite_incoming_traceparent`, `correlation_headers_work_alongside_traceparent`, `mdc_does_not_contain_trace_context_keys_set_by_filter` |
| Correlation/request behavior still works | 8 pre-existing tests unchanged |
| MDC cleanup verified | `mdc_is_cleared_after_successful_request`, `mdc_is_cleared_even_when_chain_throws` |
| No external infrastructure | All tests use `MockHttpServletRequest`/`MockHttpServletResponse` — pure JUnit, no Docker/Kafka/Postgres/Keycloak/OTLP/Schema Registry |

Test suite: `CorrelationFilterTest` (12 tests: 8 existing + 4 new) + `PlatformExceptionHandlerTest` (4) + `SafeLoggableTest` (3) + `CorrelationIdTest` (9) + `CorrelationIdTest` cross-checks for `CausationId`, `MessageId`, `RequestId`.

---

## 7. Documentation: PASS

| Criterion | Evidence |
|-----------|----------|
| Implementation note accurate | Lists all changes: new `TraceContextHeaders`, enhanced `StructuredLogFields` javadoc, `order-service/application.yml` update, `platform/README.md` observability section, `AGENTS.md`/`CLAUDE.md` guardrails. 4 new tests with purpose. 5 deferred items. 3 open questions. |
| ADR-0008 reflected consistently | Referenced in `TraceContextHeaders` javadoc, `RNF-0001`, implementation note concept map, `AGENTS.md`, `CLAUDE.md` |
| Open questions valid | 1. OTel MDC bridge trace ID format (W3C 128-bit hex vs Zipkin). 2. `micrometer-tracing-bridge-otel` dependency ownership. 3. Prometheus scrape configuration. |

---

## Observability Concerns: None

No B3 headers, no traceparent touching in CorrelationFilter, no premature OTel wiring, no unsafe logging in exception handler. Header taxonomy (`traceparent` = technical tracing, `X-Correlation-Id` = business identity, `X-Request-Id` = request identity) is clearly documented and consistently enforced across 5 documentation files.

---

## Warnings: None

**Minor observation (not actionable in ARB-009):** `AttributeNames.java` and `ObservationNames.java` (ARB-004, pre-existing) contain domain-adjacent names like `arbitrier.order.id` and `order-service.place-bulk-order`. These are telemetry naming conventions in the platform library — already noted in the ARB-004 review as acceptable.

---

## Decision

**ARB-009 may be marked [DONE].** No blockers, no warnings, no code changes needed. The W3C-only observability foundation is correctly established with proper test coverage and documentation guardrails.
