# ADR-0007 — Spring AOT / GraalVM Native Image

| Field  | Value    |
|--------|----------|
| Status | Accepted |
| Date   | 2026-07-07 |

## Intention

Establish Native Image as a first-class, supported runtime variant for Arbitrier services alongside the standard JVM mode, and record the constraints that all future implementation work must respect to remain native-compatible.

## Context

Arbitrier targets Java 25 and Spring Boot 4.1.0. Spring Boot 4.x ships with first-class Spring AOT (Ahead-of-Time) processing and GraalVM Native Image support. Native Image compilation produces a self-contained executable with sub-second startup, reduced memory footprint, and no JVM warm-up — valuable for Kubernetes deployments where cold-start matters.

However, Native Image imposes a closed-world assumption: all code paths, reflective accesses, dynamic proxies, and resources must be known at build time. Libraries that rely on runtime bytecode generation, classpath scanning, or unrestricted reflection require explicit native hints (`@RegisterReflectionForBinding`, `RuntimeHintsRegistrar`, `reflect-config.json`, etc.).

The Arbitrier stack includes several libraries with known or suspected native-compatibility concerns:

- Hibernate / JPA (runtime proxy generation for lazy loading, CGLIB entity proxies)
- Kafka consumer/producer (reflection-heavy serialization, Avro-generated classes)
- Confluent Schema Registry client (dynamic class loading for Avro codegen)
- Keycloak adapter (OIDC JWKS fetching, JWT parsing)
- Resilience4j (AspectJ / proxy-based interceptors)
- OpenTelemetry SDK (agent vs SDK split; OTLP exporters)
- Logback / SLF4J (service loader, XML config)
- Spring Security (proxy chains, SpEL for `@PreAuthorize`)

## Decision

Arbitrier supports two runtime modes:

1. **JVM mode** — standard JVM execution. Used for local development, CI unit/integration tests, and as the reference runtime.
2. **Native Image mode** — GraalVM-compiled executable. Used for performance experiments and production deployment candidates.

Both modes must be able to execute UC-01 end-to-end. Native Image mode is not required for every CI run but must not be broken by implementation choices.

Native Image compatibility is a **cross-cutting constraint**: future backend tasks must avoid native-hostile patterns unless they provide the necessary runtime hints or document an `OPEN QUESTION`.

## Consequences

- Every new dependency introduced to any `server/` module must be checked for native compatibility.
- No unconstrained reflection, dynamic class loading, or runtime proxy generation without a registered `RuntimeHintsRegistrar` or equivalent native hint.
- Spring Data JPA, Hibernate, Kafka, and Avro serialization will require explicit hints documented at the time they are added.
- Native build is not activated in CI until a dedicated task enables and validates it.
- The GraalVM `native-maven-plugin` is not added to any POM in this task — deferred to the native build activation task.

## Accepted Constraints

| Constraint | Scope |
|------------|-------|
| No unrestricted `Class.forName()` or `Method.invoke()` without hints | All `server/` modules |
| No runtime-generated CGLIB proxies in `domain/` or `application/` | Domain and application layers |
| All JPA entity classes must be registered for reflection | Persistence adapters |
| All Avro-generated classes must be registered for reflection and serialization | `contracts/` module |
| Resource files read at runtime (`application.yml`, Flyway scripts) must be registered | All modules |
| Spring `@Configuration` classes must be AOT-processable (no `@Conditional` on runtime env unless Spring-AOT-aware) | All `config/` packages |

## Native Image Risks

| Library | Risk | Mitigation Strategy |
|---------|------|---------------------|
| Hibernate 7 | Runtime CGLIB proxies for lazy loading; `@Entity` reflection | Use `spring-boot-starter-data-jpa` native hints; register all entities; consider `EAGER` loading where proxy-free |
| Kafka + Avro | Avro generated classes use reflection; Schema Registry client uses dynamic class loading | Register generated classes; validate with `avro-native-hints` or manual `reflect-config.json` |
| Keycloak adapter | JWKS endpoint client, JWT libraries, possibly Bouncy Castle | Validate against Spring Security OAuth2 native support in Boot 4 |
| Resilience4j | Annotation-driven aspects require Spring AOP proxy support | Confirm Spring AOT processes Resilience4j `@CircuitBreaker` annotations; fallback to programmatic API if not |
| OpenTelemetry | OTLP gRPC exporter uses Netty; agent mode incompatible with native | Use SDK mode only (no Java agent); validate OTLP HTTP exporter for native |
| Logback | XML config via ServiceLoader; MDC | Use `logback-spring.xml` with Spring Boot integration; validate MDC behavior |
| Spring Security | `@PreAuthorize` SpEL evaluated at runtime | Validate AOT compilation of SpEL expressions in Boot 4; register SpEL types |

## Development Mode vs Deployment Mode

| Aspect | JVM Mode | Native Image Mode |
|--------|----------|-------------------|
| Startup time | 3–10 s (Kubernetes) | < 200 ms target |
| Memory baseline | ~256 MB per service | ~64–128 MB target (OPEN QUESTION) |
| Build time | Seconds (incremental) | Minutes (full AOT + native compilation) |
| Debug tooling | Full JVM attach, hot reload, JProfiler | Limited; GDB only; no JVM introspection |
| Reflection | Unrestricted at runtime | Closed-world; hints required |
| CI use | Every run | Dedicated native CI job (future task) |
| Docker base image | Eclipse Temurin 25 | `scratch` or distroless (OPEN QUESTION) |
| GraalVM toolchain | Not required | GraalVM CE 25 or Oracle GraalVM 25 |

## Rollback Strategy

If a library or integration proves incompatible with Native Image and no hint solution exists within acceptable effort:

1. Document the incompatibility as an `OPEN QUESTION` in the relevant task's implementation note.
2. Exclude the affected module from native build targets using Maven profiles.
3. The JVM mode remains the primary deployment target until the incompatibility is resolved.
4. Record the decision to defer in a follow-up ADR update.

## Libraries to Validate (Pre-Native Build Task)

Before activating native build in CI, each of the following must be validated with a smoke test in native mode:

- [ ] `spring-boot-starter-data-jpa` + Hibernate 7 with at least one `@Entity`
- [ ] `spring-kafka` + Confluent Avro serializer with one Avro schema
- [ ] `spring-security-oauth2-resource-server` + Keycloak JWKS
- [ ] `resilience4j-spring-boot3` circuit breaker on one service method
- [ ] OpenTelemetry SDK + OTLP HTTP exporter
- [ ] Logback with `logback-spring.xml`

## Open Questions

- OPEN QUESTION: Should all services support native mode, or only a subset (e.g., order-service and orchestrator-service)?
- OPEN QUESTION: What is the GraalVM version (CE vs Oracle) required for Java 25 compatibility?
- OPEN QUESTION: Target memory baseline per service in native mode.
- OPEN QUESTION: Native Docker base image strategy (`scratch`, `distroless/java-base`, or `chainguard`).
- OPEN QUESTION: Can Hibernate lazy loading proxies be avoided entirely, or must we register CGLib proxy hints?
- OPEN QUESTION: Does Confluent Schema Registry Java client have published native hints or an alternate native-compatible client?
