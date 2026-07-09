# AGENTS.md

## Build & Test

```bash
mvn -B verify --no-transfer-progress                                        # all server modules
mvn -B verify --no-transfer-progress -pl server/order-service               # single module
mvn -B test -pl server/order-service -Dtest=ArchitectureTest                # single class
mvn -B test -pl server/order-service -Dtest=ArchitectureTest#domain*        # single method
mvn -B generate-sources --no-transfer-progress -pl server/contracts         # regenerate Avro types

docker compose -f infra/docker/docker-compose.yml up -d                     # Kafka, Postgres, Keycloak, Schema Registry
docker compose -f infra/docker/docker-compose.yml down -v
```

Include `server/platform` and `server/contracts` in `-pl` when testing any service — they are snapshot dependencies that Maven needs at the same time. See CLAUDE.md for the correct `-pl` patterns.

## Module Build Order (enforced by `server/pom.xml`)

```
platform (library) → contracts (library) → [order-service | inventory-service | credit-service | orchestrator-service]
```

`platform` must never import business-domain types (`com.arbitrier.order.*`, `.inventory.*`, `.credit.*`, `.orchestrator.*`).

## Critical Constraints (Agents Miss These)

- **Domain is pure Java** — zero Spring/JPA/Kafka imports. Domain models are immutable (new instance per transition, no setters).
- **Application services follow a fixed pipeline**: validate → derive → execute domain → persist → publish → return. Outcome always comes from the aggregate's `.status()`, never a parallel variable.
- **Kafka activation**: `KafkaPublisherConfiguration` is gated by `@ConditionalOnProperty("spring.kafka.bootstrap-servers")`. Tests (no bootstrap-servers set) skip Kafka beans and use test adapters instead — this is silent.
- **Avro serializer is not wired** — `ByteArraySerializer` is configured. Choose Confluent `KafkaAvroSerializer` before real Kafka deployment.
- **No JPA yet** — when adding the first `@Entity`, uncomment JPA deps in the service POM and register a `RuntimeHintsRegistrar` for GraalVM.
- **Resilience4j** is chosen — do not add Hystrix or Spring Retry.
- **JWT auth only in order-service** — other services lack `spring-boot-starter-oauth2-resource-server`.
- **`submittedByUserId` must NOT be in request body** — derived from JWT subject (`authentication.getName()`).
- **Spring Boot 4.1 quirks**: no `@WebMvcTest` (use `@SpringBootTest` + `MockMvc`), no bean override (use `@Primary`), no auto-configured `KafkaTemplate`. See CLAUDE.md for patterns.
- **Domain events are pure Java records** — no Avro/Kafka in domain event classes. Avro schemas live in `contracts/` and are generated at compile time.
- **Configuration bean binding**: prefer interface return types on `@Bean` methods (`ReserveCreditUseCase` not `ReserveCreditService`) — hexagonal convention.
- **REJECTED release is a no-op**: re leasing a REJECTED credit reservation does nothing (no event, no state change). This is by design — the reservation never held credit. Domain model would throw; the application service guards against it.
- **Currency mismatch is undocumented behavior**: `Money` comparison in `ReserveCreditService` compares `BigDecimal` amounts only, without checking currency. Marked as OPEN QUESTION in docs.

## Layer Enforcement

Every service follows hexagonal architecture. ArchitectureTest enforces 5 rules per service:
1. domain → adapter (no dependency)
2. application → adapter (no dependency)
3. domain → Spring/JPA (no dependency)
4. domain → Avro/Kafka (no dependency)
5. application → Avro/Kafka (no dependency)

## Documentation Workflow

- Read OKF → RF → RNF → ADR → test-case docs before coding.
- Do not invent business behavior. Mark missing details as `OPEN QUESTION` (all caps).
- Implementation notes go in `docs/implementation/ARB-NNN-task-name.md`.
- Review reports go in `docs/implementation/ARB-NNN-task-name-REVIEW.md`.

## References

- **CLAUDE.md** — detailed style guide, naming conventions, Spring Boot 4.1 patterns, application service design, saga states, testing expectations.
- **docs/adr/** — Architecture Decision Records (trace context, outbox, Avro, schema-per-service, GraalVM).
- **docs/rnf/** — Non-functional requirements (technical baseline, native image runtime).
- **docs/test-cases/** — Test case specs for UC-01 (12 test cases, TC-UC-01-001 through TC-UC-01-012).
