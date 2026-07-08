# ARB-008 — Shared Platform Spring Integration

| Field  | Value       |
|--------|-------------|
| Task   | ARB-008     |
| Status | Implemented |
| Date   | 2026-07-07  |

## Files Created

### `platform/web`

| File | Purpose |
|------|---------|
| `CorrelationHeaders` | `X-Correlation-Id` and `X-Request-Id` header name constants |
| `CorrelationFilter` | Servlet filter: propagates or generates `correlationId`, always generates `requestId`, populates/clears MDC |
| `ProblemResponse` | Uniform JSON error body record (`code`, `message`, `status`) |
| `PlatformExceptionHandler` | `@RestControllerAdvice` mapping `ApplicationProblemException` → 422, `IllegalArgumentException` → 400, `MethodArgumentNotValidException` → 400 |

### `platform/spring`

| File | Purpose |
|------|---------|
| `PlatformAutoConfiguration` | `@AutoConfiguration` + `@ConditionalOnWebApplication(SERVLET)` — registers filter, exception handler, and `TimeProvider` bean |

### Auto-configuration registration

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` registers `PlatformAutoConfiguration`. All services with `spring-boot-starter-web` pick it up automatically.

## Correlation Convention

| Header | Direction | Behaviour |
|--------|-----------|-----------|
| `X-Correlation-Id` | Request → Response | Preserved from client; generated if absent |
| `X-Request-Id` | Response only | Always generated per request |

MDC keys populated:

| MDC key | Value |
|---------|-------|
| `correlationId` | From `StructuredLogFields.CORRELATION_ID` |
| `requestId` | From `StructuredLogFields.REQUEST_ID` |

## Error Mapping Convention

| Exception | HTTP Status | Response code |
|-----------|-------------|---------------|
| `ApplicationProblemException` | 422 | `ex.code().code()` |
| `IllegalArgumentException` | 400 | `"VALIDATION_ERROR"` |
| `MethodArgumentNotValidException` | 400 | `"VALIDATION_ERROR"` with field detail |

## TimeProvider Bean

`PlatformAutoConfiguration` provides `SystemClock.INSTANCE` as a `TimeProvider` bean with `@ConditionalOnMissingBean`. Services override it in their `config/` package or `@TestConfiguration` to use `FixedTimeProvider` in tests.

## What Was Intentionally Not Implemented

| Area | Reason |
|------|--------|
| Keycloak / Spring Security | Deferred to security integration phase |
| `X-Tenant-Id` header | Out of scope for v1 (single-tenant) |
| OTLP / OpenTelemetry wiring | Deferred to observability phase |
| Outbox / Inbox support | Deferred to Kafka adapter phase |
| Custom `NullPointerException` handler | NPE is a programming error → default 500 is correct |

## Native Image Considerations

- `CorrelationFilter` implements `jakarta.servlet.Filter` directly — no reflection, no CGLIB proxies. Spring AOT will process it correctly.
- `@RestControllerAdvice` annotation is processed by Spring AOT at build time. No hint needed.
- `FilterRegistrationBean` is Spring-Boot-AOT-aware.
- `@ConditionalOnMissingBean` / `@ConditionalOnWebApplication` are standard Boot conditions with known AOT support.

## Open Questions

- OPEN QUESTION: Should `correlationId` be propagated into Kafka message headers when the first Kafka producer is implemented?
- RESOLVED (ADR-0008): W3C Trace Context (`traceparent`/`tracestate`) is the propagation standard. `X-B3-TraceId` support is not added; B3 is only a fallback for legacy interoperability documented at the integration boundary.
- OPEN QUESTION: Should `PlatformExceptionHandler` be made `@ConditionalOnMissingBean` to allow services to provide their own advice?
