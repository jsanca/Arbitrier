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
