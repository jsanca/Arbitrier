# Arbitrier Knowledge Base

## Purpose

This Knowledge Base is the navigable map of Arbitrier's durable, current
conceptual knowledge. It connects authoritative sources; it does not replace
their decisions, requirements, contracts, code, tests, or historical evidence.

## Documentation Boundaries

- [README](../README.md) is the repository entry point and onboarding guide.
- [`docs/engineering/`](../docs/engineering/documentation-ownership.md) owns
  engineering governance, policy, and documentation ownership.
- [`docs/agents/`](../docs/agents/) preserves task instructions and historical
  delivery evidence; [ENGINEERING_LOG](../ENGINEERING_LOG.md) indexes that evidence.
- `knowledge/` maps durable concepts and their authorities.

Knowledge should be more stable than implementation. Prefer bounded contexts,
workflows, contracts, persistent concepts, operational concepts, and shared
terms over temporary task narratives, individual implementation classes, or
one-off refactor details.

## Source of Truth

Every page added here must identify and link to its source of truth. Accepted
ADRs, RF/RNF documents, schemas, code, and tests remain authoritative for
their respective concerns. A knowledge page may summarize or connect them, but
must not silently override them. Report conflicts rather than selecting a
winner without an explicit decision.

## Navigate

- [Architecture](architecture/index.md) — stable boundaries and decisions.
- [Domain](domain/index.md) — bounded contexts and durable business concepts.
- [Contracts](contracts/index.md) — APIs, messages, schemas, and integrations.
- [Data](data/index.md) — persistent and dataset-level concepts.
- [Operations](operations/index.md) — runtime and observability concepts.
- [Glossary](glossary/index.md) — shared project terminology.

Each section index is intentionally lightweight. Add child pages only when a
durable concept needs a curated navigation point beyond its authoritative
artifact.
