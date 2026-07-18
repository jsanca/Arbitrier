# Operations Knowledge

## Purpose

Navigate durable runtime, observability, and operational concepts without
duplicating runtime configuration, dashboards, or transient environment state.

## Scope

Future pages may cover operational concepts, metrics, traces, health behavior,
and runbook-level navigation when supported by current runtime documentation.

## Authoritative Sources

- [Technical baseline RNF](../../docs/rnf/RNF-0001-technical-baseline.md)
- [Saga runtime RNF](../../docs/rnf/RNF-UC-01-saga-runtime.md)
- [Trace-context ADR](../../docs/adr/ADR-0008-w3c-trace-context-propagation.md)
- [Outbox, inbox, and idempotency ADR](../../docs/adr/ADR-0005-outbox-inbox-idempotency.md)
- [Local runtime guide](../../infra/docker/README.md)

## Navigation

- [Concurrent Dispatch Runtime](concurrent-dispatch-runtime.md) — durable
  outbox-claim lifecycle, worker ownership, database coordination, and polling
  boundaries.

Add metrics, recovery, retry, and tuning pages incrementally from verified
operational behavior, not from aspirational configuration.
