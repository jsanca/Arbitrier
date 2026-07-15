## Proposed Knowledge Base

```text
knowledge/
  index.md
  architecture/
    index.md
  domain/
    index.md
    <bounded-context>/
      index.md
      aggregates/
        index.md
      value-objects/
        index.md
  workflows/
    index.md
  contracts/
    index.md
    apis/
      index.md
    commands/
      index.md
    events/
      index.md
    queries/
      index.md
    schemas/
      index.md
  data/
    index.md
    datasets/
      index.md
    tables/
      index.md
  operations/
    index.md
    metrics/
      index.md
  glossary/
    index.md
```

Folders are created only when they have real content; each created folder has an `index.md`.

| Section | Purpose |
|---|---|
| `architecture` | Stable component/boundary map; links to ADRs rather than duplicating decisions. |
| `domain` | Bounded contexts and their durable concepts: aggregates and value objects. |
| `workflows` | Cross-context business flows, sagas, and lifecycle diagrams. |
| `contracts` | Externally consumed interfaces: APIs, commands, queries, events, and schemas. |
| `data` | Business datasets and persistent-table reference material where useful. |
| `operations` | Observable runtime concepts, starting with metrics. |
| `glossary` | Shared ubiquitous language and unambiguous terminology. |

## Classification Recommendation

| Artifact | Location | Canonical? |
|---|---|---|
| Aggregates, value objects | `domain/<context>/` | Yes, when meaningful to the domain |
| Commands, queries, events | `contracts/` | Yes |
| APIs, schemas | `contracts/` | Yes |
| Workflows/sagas | `workflows/` | Yes |
| Datasets, tables | `data/` | Optional, but supported canonically |
| Metrics | `operations/metrics/` | Optional, but supported canonically |
| Ports, adapters | `architecture/` | Project-specific; document stable boundaries, not every implementation class |
| Glossary | `glossary/` | Yes |

This makes the taxonomy reusable without forcing microservices, databases, messaging, or DDD implementation details onto every project.

## Index Convention

Every `index.md` should contain:

```md
---
title: Contracts
type: index
owner: Architecture Team
status: active
source_of_truth: self
last_reviewed: YYYY-MM-DD
---

# Contracts

## Purpose

## Navigation

## Ownership

## Relationships
```

Navigation lists child pages; ownership identifies the accountable role/team; relationships link upstream authority and related knowledge pages. An index is a map, not a duplicate summary of every child.

## Knowledge Document Template

```md
---
title: <Human-readable title>
type: <aggregate | event | api | workflow | metric | glossary-term | ...>
owner: <team or role>
bounded_context: <context | global>
source_of_truth: <relative path, generated schema, or self>
tags: [<tag>]
status: <draft | active | deprecated | superseded>
last_reviewed: YYYY-MM-DD
---

# <Title>

## Purpose

## Definition

## Relationships

## Source and Evidence

## Notes
```

Use frontmatter partially, not as a complete OKF-style metadata system. The listed fields are enough for people and AI agents to find authority, scope, freshness, and relationships. IDs, versioning, authorship, lifecycle timestamps, and tool-specific fields should remain optional.

## Documentation Ownership

- `docs/engineering/`: role-independent engineering policy, process, quality standards, and documentation governance.
- `docs/agents/`: task instructions, implementation/review/fix evidence, checkpoints, and templates. This is historical delivery memory.
- `knowledge/`: navigable, current conceptual/reference knowledge. It links to authoritative material and must not silently restate or override it.
- Existing `docs/adr/`, `docs/rf/`, `docs/rnf/`, test cases, service READMEs, and source schemas remain authoritative for their respective concerns unless explicitly superseded.

A knowledge page should use `source_of_truth` to point to the authority it summarizes. `self` is appropriate only for curated material such as glossary definitions or a cross-source navigation page.

## Migration Strategy

1. Approve the taxonomy and ownership model; record adoption as an ADR if it changes documentation governance.
2. Create only `knowledge/index.md` initially, then add section indexes as content is introduced.
3. Seed link-first pages for the highest-value concepts—bounded contexts, UC workflows, contracts, and glossary terms.
4. Do not move ADRs, RF/RNF, reports, schemas, or test cases. Link to them.
5. Add knowledge-page updates to feature delivery only when the implementation changes a durable concept or contract.
6. Periodically curate broken links, stale `last_reviewed` values, orphaned pages, missing indexes, missing ownership, and terminology drift.

## Knowledge Curator Responsibilities

The Knowledge Curator maintains navigability and evidence integrity: detect broken or orphaned links, stale content, missing indexes, missing ownership, absent pages for implemented durable concepts, and conflicting terminology. The role must not invent domain rules, rewrite historical delivery evidence, or claim current behavior without source evidence.

One tracking concern: the repository already contains a completed, different `ARB-KNOW-002` record. This proposed task should receive a distinct identifier before it is added to durable reporting, avoiding a collision.