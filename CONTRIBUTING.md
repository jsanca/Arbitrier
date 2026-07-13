# Contributing to Arbitrier

## Coding Conventions

### Hexagonal Architecture

Each service module is strictly layered. Dependencies flow inward only:

```
adapter → application → domain
config  → application
```

- `domain` must not import Spring, JPA, or Kafka classes.
- `application` must not import adapter classes.
- `adapter` must only communicate with `application` through ports.

### `package-info.java` Policy

**Every package must have a `package-info.java` file.**

Minimum content:

```java
/**
 * <One-sentence purpose of this package.>
 *
 * <p>Layer: [domain | application | adapter | config]
 * <p>Module: <service-name>
 */
package com.arbitrier.<service>.<layer>[.<sublayer>];
```

Rationale:
- Keeps package intent explicit as the codebase grows.
- Required for complete Javadoc generation.
- Acts as the first thing a new contributor reads when navigating a package.

### Naming Conventions

| Artifact                   | Convention                                  |
|----------------------------|---------------------------------------------|
| Input ports (use cases)    | `<Action><Subject>UseCase` (interface)      |
| Output ports               | `<Subject>Repository`, `<Subject>Gateway`   |
| Application services       | `<Action><Subject>Service`                  |
| REST controllers           | `<Subject>RestAdapter`                      |
| JPA repositories           | `<Subject>JpaAdapter`                       |
| Kafka consumers            | `<Subject>KafkaConsumerAdapter`             |
| Kafka producers            | `<Subject>KafkaProducerAdapter`             |
| Domain events              | `<Subject><PastTense>Event`                 |
| Avro schemas               | `<subject>-<past-tense>-event-v<N>.avsc`    |

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short description>

[optional body]
[optional footer]
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `ci`  
Scopes: `order-service`, `inventory-service`, `credit-service`, `orchestrator-service`, `contracts`, `platform`, `client`, `infra`

### Testing Pyramid

| Level           | Tool                    | Location                        |
|-----------------|-------------------------|---------------------------------|
| Unit            | JUnit 5 + Mockito       | `src/test/java`                 |
| Integration     | Spring Boot Test + Testcontainers | `src/test/java`       |
| Contract        | Avro Schema Registry        | `contracts/`               |
| E2E             | Playwright              | `client/e2e/`                   |

## Documentation Standards

- Functional Requirements → `docs/rf/RF-XXXX-<slug>.md`
- Non-Functional Requirements → `docs/rnf/RNF-XXXX-<slug>.md`
- Architecture Decisions → `docs/adr/ADR-XXXX-<slug>.md`
- Test Cases → `docs/test-cases/TC-<UC-ID>-<slug>.md`
- Update `docs/okf/index.md` when adding a new major document.

## Engineering Workflow

```text
Idea → ADR → Task → Implementation → Deep Review → Fix → Done → Documentation
```

1. Capture the business or technical idea without inventing missing rules.
2. Write or amend an ADR when the change establishes a cross-boundary architectural decision.
3. Create a bounded task with acceptance criteria and explicit exclusions.
4. Implement the slice with tests and an implementation report.
5. Run an independent Deep review and preserve its report as historical evidence.
6. Resolve material findings in a focused fix task.
7. Mark roadmap status done only when implementation and required review/fixes are complete.
8. Refresh current-state documentation; do not rewrite historical reports as if they described the latest repository.

Project responsibilities:

| Role | Responsibility |
|---|---|
| Clio | Backend/domain/application implementation and tests |
| Deep | Independent architecture, correctness, test, and documentation review |
| Brio | Customer Portal implementation and frontend tests |
| Stitch | Visual exploration and mockups used as design inputs |
| Elito | Infrastructure, documentation coherence, evaluation, and integration |
