# RNF-0002 — Native Image Runtime

| Field  | Value    |
|--------|----------|
| Status | Draft    |
| Date   | 2026-07-07 |

## Intention

Define the non-functional requirements that Arbitrier services must satisfy when compiled to a GraalVM Native Image executable.

## Context

ADR-0007 establishes Native Image as a supported runtime variant. This document translates that decision into measurable, testable constraints for startup, memory, build, and observability behavior.

## Decision or Requirement

Services compiled with `spring-boot-starter-data-jpa`, Spring AOT, and the GraalVM `native-maven-plugin` must meet the targets below. Requirements marked `OPEN QUESTION` are aspirational until validated.

## Inputs

- ADR-0007 (GraalVM Native Image decision and risk register)
- RNF-0001 (Technical Baseline — JVM constraints that native must not violate)

## Outputs

- Measurable acceptance criteria for startup, memory, build, and observability in native mode.

## Preconditions

- GraalVM CE 25 or Oracle GraalVM 25 is available in the CI build environment.
- `native-maven-plugin` is configured (deferred to native build activation task).
- A smoke-test suite exists that exercises at minimum one HTTP endpoint and one Kafka consumer per service.

## Postconditions

- Native executables start, pass health checks, and handle requests.
- Observability signals (logs, traces, metrics) are emitted correctly in native mode.
- No fallback to JVM mode silently occurs at runtime.

## Failure Behavior

- If a native executable fails to start, the CI pipeline fails and no deployment proceeds.
- If a library produces an incompatibility with no available hint, document the incompatibility per ADR-0007 rollback strategy and exclude the module from the native build target.

## Observability Expectations

- Structured logs include `sagaId`, `orderId`, `traceId` in native mode — identical to JVM mode.
- OTLP HTTP exporter (not the Java agent) must be used; span export must be validated in native smoke tests.
- Micrometer Prometheus endpoint (`/actuator/prometheus`) must be functional in native mode.

## Test Evidence Placeholder

- Native smoke test results pending native build activation task.
- Startup time and RSS measurements pending native CI job.

## Open Questions

- OPEN QUESTION: Exact startup time target per service.
- OPEN QUESTION: Exact RSS (resident set size) target per service.
- OPEN QUESTION: Whether native smoke tests run in PR CI or only in nightly/release builds.
- OPEN QUESTION: Which GraalVM distribution and version to pin in CI.

---

## 1. Startup Expectations

| Requirement | Target | Notes |
|-------------|--------|-------|
| Startup time (native) | < 200 ms | Measured from process start to first health-check response |
| Startup time (JVM, for comparison) | < 10 s | Existing JVM baseline; native must not exceed this |
| Health check readiness | `/actuator/health` responds within 500 ms of start | OPEN QUESTION: exact SLA |

---

## 2. Memory Expectations

| Requirement | Target | Notes |
|-------------|--------|-------|
| RSS at idle | < 128 MB per service | OPEN QUESTION — validate after first native build |
| RSS under load | OPEN QUESTION | Measure during smoke test with simulated saga traffic |
| Heap limit (native GC) | Configured via `-Xmx` at deploy time | No JVM ergonomics; must be set explicitly in Kubernetes `resources.limits` |

---

## 3. Build-Time Expectations

| Requirement | Value | Notes |
|-------------|-------|-------|
| Native compilation tool | GraalVM CE 25 `native-image` or Oracle GraalVM 25 | OPEN QUESTION: pin exact version |
| Maven plugin | `org.graalvm.buildtools:native-maven-plugin` | Not added to POM until native build activation task |
| Build time target | < 10 min per service (cold build) | Native compilation is inherently slow; acceptable in CI release jobs |
| Build reproducibility | Deterministic output for same source + GraalVM version | Required for artifact signing |
| Spring AOT processing | `spring-boot:process-aot` runs before `native:compile` | Standard Spring Boot 4 pipeline |

---

## 4. Native Smoke Test Expectations

A native smoke test is a minimal integration test that:

1. Starts the native executable against a real (Testcontainers or docker-compose) PostgreSQL and Kafka instance.
2. Exercises the primary inbound HTTP endpoint for at least one UC-01 step.
3. Verifies that the response is correct and the health endpoint returns `UP`.
4. Verifies at least one structured log line with `sagaId`, `orderId`, `traceId`.
5. Verifies at least one OpenTelemetry span is exported via OTLP HTTP.

Smoke tests do not replace full integration tests. They validate that the binary starts and handles a request without crashing.

---

## 5. Reflection, Proxy, and Resource Constraints

| Category | Constraint |
|----------|------------|
| Reflection | All classes accessed via reflection must be registered with `@RegisterReflectionForBinding` or a `RuntimeHintsRegistrar` |
| JDK dynamic proxies | Allowed only if the interface list is registered via `RuntimeHints.proxies()` |
| CGLIB proxies | Not permitted in `domain/` or `application/` layers; must be avoided or replaced with interface proxies in `adapter/` and `config/` |
| Serialization | Avro-generated classes and all Kafka message types must be registered for Java serialization if used |
| Resources | `application.yml`, Flyway migration scripts, Avro `.avsc` files, Keycloak JWKS caches must be registered as native resources |
| Class loading | `Class.forName()` is forbidden without a registered hint; dynamic classloading from external jars is incompatible with native |
| `@SpringBootApplication` scanning | Must complete AOT processing; no `@Conditional` that evaluates external state at runtime without AOT awareness |

---

## 6. Observability Constraints

| Signal | JVM Mode | Native Mode | Constraint |
|--------|----------|-------------|------------|
| Traces | Java agent or SDK | SDK only (OTLP HTTP) | No Java agent in native; use `opentelemetry-spring-boot-starter` SDK wiring |
| Metrics | Micrometer + Prometheus | Same | Validate `MeterRegistry` beans are AOT-processable |
| Logs | SLF4J → Logback | SLF4J → Logback | Use `logback-spring.xml`; validate MDC propagation in native |
| Health | Spring Actuator | Same | `/actuator/health` must be registered as a native endpoint |
| OTLP exporter | gRPC or HTTP | HTTP only (Netty gRPC may not compile) | OPEN QUESTION: validate gRPC exporter native support in Boot 4 |

---

## 7. CI/CD Constraints

| Constraint | Notes |
|------------|-------|
| Native builds do not run on every PR | Too slow; run in dedicated nightly or release pipeline |
| JVM build remains the PR gate | Every PR must pass JVM build; native is additive |
| Native CI job uses GraalVM CE 25 Docker image | Pinned version; OPEN QUESTION: exact image tag |
| Docker image for native deployment | Distroless or `scratch`; OPEN QUESTION: exact base |
| Artifact naming | `<service>-native` image tag to distinguish from JVM image | 
| Rollback | If native image fails smoke test, deploy JVM image; record failure in issue tracker |
