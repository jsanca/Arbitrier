Task: ARB-004-FIX-001 — Resolve Deep Review Warnings

Status:
[PLANNED]

Owner:
Clio

Context:
Deep reviewed ARB-004 (Platform Foundation) with:

PASS WITH WARNINGS

Goal:
Resolve only the architectural cleanup items identified during the review.

Changes:

1. server/platform/pom.xml

- Remove spring-boot-starter-web.
- Platform must remain a pure library module.
- Do not introduce any Spring MVC dependency.
- Do not add Spring Security.
- Do not add any runtime infrastructure.

2. PlatformArchitectureTest

- Activate the currently commented ArchUnit rule(s).
- Verify that platform remains independent from:
    - order-service
    - inventory-service
    - credit-service
    - orchestrator-service

- The architecture test must execute as part of the normal test suite.

3. Documentation

- Update docs/implementation/ARB-004-platform-foundation.md
  with a small "Deep Review Fixes" section documenting:

    - spring-boot-starter-web removed
    - PlatformArchitectureTest activated

Do NOT:

- Change ObservationNames.
- Rename observation constants.
- Introduce new abstractions.
- Add flatMap to Result.
- Modify APIs.
- Add business domain classes.
- Start ARB-005.

Acceptance Criteria:

✓ platform/pom.xml no longer declares spring-boot-starter-web.

✓ PlatformArchitectureTest executes and passes.

✓ Platform remains completely domain-neutral.

✓ Documentation reflects the cleanup.

✓ No functional behavior changes.

After completion:

Report:

- Modified files.
- Test results.
- Any remaining OPEN QUESTION.

This task is cleanup only.

After successful completion, ARB-004 will be officially marked [DONE].