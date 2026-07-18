# Architecture Knowledge

## Purpose

Navigate stable architectural boundaries, patterns, and decisions without
duplicating Architecture Decision Records.

## Scope

Future pages may cover bounded-context relationships, architectural patterns,
and cross-cutting technical concepts that remain meaningful across individual
implementation changes. Do not catalog classes, packages, or task mechanics
unless they are themselves a stable public boundary.

## Authoritative Sources

- [Project structure ADR](../../docs/adr/ADR-0001-project-structure.md)
- [Orchestrated saga ADR](../../docs/adr/ADR-0002-orchestrated-saga-with-kafka.md)
- [Schema-per-service ADR](../../docs/adr/ADR-0003-schema-per-service-postgres.md)
- [Native image ADR](../../docs/adr/ADR-0007-spring-aot-graalvm-native-image.md)
- [Trace-context ADR](../../docs/adr/ADR-0008-w3c-trace-context-propagation.md)

## Navigation

- [Concurrent Dispatch Runtime](../operations/concurrent-dispatch-runtime.md) —
  database-owned message claims and transport-independent dispatch boundaries.

Add a page only when it links several stable authorities or makes a durable
relationship easier to discover.
