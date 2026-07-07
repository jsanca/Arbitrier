Task: ARB-003-FIX-001 — Resolve Deep Review Warnings

Owner:
Clio

Context:
Deep reviewed ARB-003 with PASS WITH WARNINGS.

Goal:
Apply only the two documentation/dependency cleanups requested by Deep.

Changes:
1. CONTRIBUTING.md
    - Remove Pact reference.
    - Contract testing should mention Avro Schema Registry only.

2. server/platform/pom.xml
    - Remove spring-boot-starter-security if it is currently declared.
    - Security will be reintroduced in the Keycloak/security integration phase.

Do not:
- Add business logic.
- Add security config.
- Add Kafka config.
- Add JPA config.
- Start ARB-004.

Acceptance:
- CONTRIBUTING.md no longer references Pact.
- platform/pom.xml no longer implies active security wiring.
- docs/implementation/ARB-003-architecture-skeleton.md may be updated with a small “Deep review fixes” note.