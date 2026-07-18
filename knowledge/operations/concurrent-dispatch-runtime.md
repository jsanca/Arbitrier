# Concurrent Dispatch Runtime

## Purpose

Coordinate concurrent delivery of durable outbox messages without assigning the
same pending message to more than one running worker. This is a Platform
operational concept: business bounded contexts create durable messages but do
not own their dispatch, claim, or recovery mechanics.

## Outbox Claim Lifecycle

An outbox message progresses through this current lifecycle:

```text
PENDING → CLAIMED → PUBLISHED
                  ↘ FAILED
```

- **PENDING** messages are eligible for a worker to own.
- **CLAIMED** messages have one recorded worker identity and claim timestamp.
- **PUBLISHED** messages were delivered successfully and retain no active claim.
- **FAILED** messages record a failed publication attempt and retain no active
  claim. The current runtime has no automatic retry or recovery transition.

A claim is exclusive: only a pending message can be claimed, and a claimed or
completed message cannot receive another active claim.

## Worker Ownership Model

Each running worker has an identity recorded with every claim. The identity
makes current ownership observable and distinguishes concurrent workers,
including multiple workers on the same host. A deployment may supply the
identity; otherwise the runtime produces a readable, startup-unique identity.

Worker identity is ownership metadata, not a business identity and not a
message-routing key.

## Claim-Based Dispatch

Workers acquire a bounded batch before treating messages as dispatchable work.
The returned batch contains only messages already owned by that worker and is
ordered oldest first, with a deterministic secondary ordering when timestamps
tie. Each worker processes its claimed batch sequentially; a dispatch failure
ends the remaining work for that cycle.

This separates **claiming** from **publishing**: a claim establishes the right
to attempt dispatch, not proof that the transport accepted the message.

## Database Ownership and Message Transport

The database is authoritative for message ownership and lifecycle transitions.
The message transport is responsible only for publication after ownership has
been established. This boundary supports at-least-once delivery: exclusive
ownership prevents concurrent workers from intentionally dispatching the same
pending message, while downstream consumers must still tolerate duplicate
delivery according to the outbox and idempotency decision.

## Concurrent Dispatch and SKIP LOCKED

Concurrent workers use database row ownership to receive disjoint batches.
The PostgreSQL `FOR UPDATE SKIP LOCKED` pattern locks pending candidates and
skips rows already selected by another active claimant. The selected rows are
then transitioned to `CLAIMED` in the same short transaction.

This is a persistence-coordination pattern, not a business lock. It is scoped
to selecting and claiming a bounded batch; it does not protect the later
transport operation.

## Transaction Boundary During Dispatch

Claim selection and the transition to `CLAIMED` occur in one short database
transaction. That transaction commits before transport publication begins, so
database locks are not held while a message is dispatched. The database records
the resulting `PUBLISHED` or `FAILED` state after the publication attempt.

## Polling Runtime

The polling runtime periodically starts bounded claim-and-dispatch cycles.
Within a single worker, overlapping cycles are skipped while a cycle is active.
Across workers, database claims provide the exclusive-ownership guarantee.

## Current Limits and Future Knowledge

If a worker stops after claiming a message, that message remains `CLAIMED`.
Stale-claim recovery, automatic retry, backoff, and retry scheduling are not
part of the current runtime; they are deferred to ARB-022.6.

Future knowledge pages may cover claim recovery, retry policy, delivery
observability, and operational tuning once authoritative behavior exists.

## Source of Truth

- [Outbox, inbox, and idempotency ADR](../../docs/adr/ADR-0005-outbox-inbox-idempotency.md)
- [Claim state model](../../docs/agents/reports/ARB-022.5.1-outbox-claim-state-model.md)
- [Atomic claim repository](../../docs/agents/reports/ARB-022.5.2-atomic-claim-repository.md)
- [Claim-aware batch retrieval](../../docs/agents/reports/ARB-022.5.3-claim-aware-batch-retrieval.md)
- [Multi-worker claim-based polling runtime](../../docs/agents/reports/ARB-022.5.4-multi-worker-claim-based-polling.md)
- [Platform guide](../../server/platform/README.md)

## Related Knowledge

- [Platform bounded context](../domain/platform.md)
- [Architecture knowledge](../architecture/index.md)
- [Contract knowledge](../contracts/index.md)
