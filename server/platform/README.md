# platform

Cross-cutting library jar shared by all server-side services.

## Responsibility

This module is **not a deployable service**. It is a dependency of every `*-service` module.

Contents:

| Package                               | Purpose                                                   |
|---------------------------------------|-----------------------------------------------------------|
| `com.arbitrier.platform.security`     | Keycloak JWT filter chain configuration                   |
| `com.arbitrier.platform.observability`| OpenTelemetry setup, MDC helpers (sagaId, orderId, traceId injection) |
| `com.arbitrier.platform.exception`    | Shared exception hierarchy (`ArbitrierException`, `DomainException`, etc.) |
| `com.arbitrier.platform.validation`   | Common validation utilities                               |
| `com.arbitrier.platform.web`          | Standard error response DTOs, pagination wrappers         |

## Rules

- **No business logic.** Platform must not know about orders, credit, or inventory.
- **No circular dependencies.** Services depend on platform; platform depends on nothing within this repo.
- **Backward compatibility.** Breaking changes to platform require bumping the minor version and announcing in the PR.

## Status

`ARB-001` — Structure placeholder. No implementation yet.
