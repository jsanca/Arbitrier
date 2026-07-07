Task: ARB-003 — Architecture Skeleton

Status:
[PLANNED]

Owner:
Clio

Context:
Arbitrier already has repository layout and documentation foundation.
ARB-001 and ARB-002 are complete.

Goal:
Create the initial backend architecture skeleton for all server modules without implementing business logic.

Modules in scope:
- server/order-service
- server/inventory-service
- server/credit-service
- server/orchestrator-service
- server/contracts
- server/platform

Out of scope:
- No business use cases.
- No real Kafka consumers/producers.
- No Avro schema generation.
- No JPA entities yet.
- No database migrations yet.
- No REST business endpoints yet.

Required package structure for each service:

src/main/java/<base-package>/
├── adapter/
│   ├── inbound/
│   │   ├── rest/
│   │   └── kafka/
│   └── outbound/
│       ├── persistence/
│       └── kafka/
├── application/
│   ├── port/
│   │   ├── inbound/
│   │   └── outbound/
│   └── service/
├── domain/
│   ├── model/
│   ├── event/
│   ├── command/
│   └── exception/
├── config/
├── observability/
└── package-info.java

Required package structure for platform:

server/platform/src/main/java/<base-package>/platform/
├── observability/
├── logging/
├── security/
├── kafka/
├── idempotency/
├── exception/
├── validation/
├── test/
└── package-info.java

Required package structure for contracts:

server/contracts/
├── src/main/avro/
├── src/main/java/<base-package>/contracts/
└── README.md

Deliverables:
1. Create Maven or Gradle backend skeleton.
2. Each service must have a minimal Spring Boot application class.
3. Each Java package must include package-info.java.
4. Each module must have README.md updated with its role and architecture.
5. Add a minimal health endpoint or rely on Spring Actuator if already configured.
6. Add placeholder test structure:
    - src/test/java
    - unit
    - integration
7. Add no-op architecture tests if possible.
8. Add root/server README section explaining backend module relationships.
9. Update docs/okf/index.md only if needed to reference ARB-003.
10. Add docs/implementation/ARB-003-architecture-skeleton.md or similar implementation note.

Baseline dependencies:
- Java 25
- Spring Boot 4.1.0
- Spring Web
- Spring Actuator
- Spring Validation
- Spring Data JPA placeholder only if needed
- Kafka dependency placeholder only if needed
- OpenTelemetry placeholder only if needed
- SLF4J through Spring Boot logging

Guardrails:
- Do not create domain models such as Order, OrderLine, Saga, Credit, InventoryReservation yet.
- Do not create controllers for business operations.
- Do not create Kafka topics.
- Do not create Avro schemas.
- Do not create Flyway/Liquibase migrations yet.
- Do not add hidden business logic in adapters.
- Keep everything boring, symmetrical, and easy to extend.

Acceptance Criteria:
- Backend skeleton compiles.
- All service modules are present.
- All expected packages exist.
- All packages contain package-info.java.
- Each service has a minimal Spring Boot app class.
- Tests can run even if they only validate context/package conventions.
- No business logic exists.
- No production Kafka flow exists.
- No Avro contracts exist yet.
- Documentation is updated to mark ARB-003 as implemented or ready for review.

After completion:
- Report created files.
- Report whether build passes.
- Report any open questions.
- Do not start ARB-004.

Important:
This task should make the repository ready for implementation slices, not start those slices.
Prefer clean empty architecture over premature abstractions.