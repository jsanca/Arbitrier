# ARB-DOC-001 — Documentation Audit & README Refresh

| Field | Value |
|---|---|
| Status | Complete |
| Owner | Elito |
| Date | 2026-07-10 |
| Scope | Documentation only |

## Outcome

The active documentation now describes the repository as it exists after ARB-019, ARB-UI-001, and ARB-027. The audit distinguished implemented domain/application/persistence capabilities from planned runtime integration, corrected the UC-01 customer-decision model, and made the development workflow discoverable to a new engineer.

No Java, TypeScript, infrastructure configuration, database scripts, or UI assets were changed by ARB-DOC-001.

## Reviewed material

The audit reviewed all major documentation entry points and compared their claims with the current source tree:

- root and contributor guides: `README.md`, `CONTRIBUTING.md`, `AGENTS.md`, `CLAUDE.md`;
- module documentation: `server/README.md` and every service/platform/contracts README;
- architecture: ADR-0001 through ADR-0009 and the project blueprint;
- current requirements: OKF index, UC-01, RF-UC-01, RNF-0001, RNF-0002, RNF-UC-01;
- roadmap and task statuses through ARB-027 plus ARB-UI-001;
- implementation and Deep review reports through ARB-019-era source, ARB-UI-001, and ARB-027;
- all UC-01 test-case specifications and seed documents;
- Customer Portal README, route implementation, UX navigation map, design tokens, and ARB-UI-001 report;
- local Compose guide, environment contract, service definitions, realm import, and operational scripts;
- Markdown links, referenced repository paths, stale terminology, placeholders, and open questions.

Historical implementation and Deep review reports were treated as records of their slice, not rewritten to pretend they were current-state specifications.

## Inconsistencies found and fixed

| Area | Previous inconsistency | Resolution |
|---|---|---|
| Root story | README described an early skeleton, Spring State Machine/BPMN, placeholder client, and JPA/cloud capabilities without implementation status | Replaced with a current capability/boundary narrative, architecture flow, module matrix, quick start, tests, workflow, and honest roadmap gaps |
| Saga/customer decision | Active UC/RF/RNF and ADR-0002 placed `AWAITING_CUSTOMER_DECISION` inside the running saga | Reconciled with ARB-017: partial availability is resolved before order/saga submission; legacy enum/contract artifacts are identified as inactive |
| Inventory ownership | Active requirements exposed warehouse availability to callers | Reconciled with ADR-0009 and ARB-017B: global quantities cross the boundary; warehouse selection and allocations remain inside Inventory |
| Retry terminology | “Timeout policy” implied duration/backoff behavior | Described the implemented artifact as an attempt-count retry decision policy; ARB-024 retains runtime scheduling, duration, and backoff |
| Persistence | Order, Inventory, Credit, Orchestrator, and server READMEs claimed repositories were in-memory only/no JPA | Documented separate JPA models/mappers/adapters, optimistic locking, integration tests, and application-service transaction ownership |
| Contracts | Contracts README claimed no Avro files existed | Documented the 26 schemas, generated types, compatibility boundary, and remaining runtime serializer/adapters |
| Roadmap | ARB-027 remained planned; ARB-017B and ARB-UI-001 were absent; ARB-026 implied the prototype had not started | Synchronized completed and planned slices and added ARB-DOC-001 |
| Task headers | Completed task specifications still displayed `[PLANNED]` | Updated implemented primary tasks ARB-003 through ARB-019 and ARB-UI-001 to `[COMPLETE]`; ARB-020 remains planned |
| Identity | RNF role names conflicted with the imported local Keycloak realm | Aligned active local role documentation to `CUSTOMER_*` and `OPERATIONS_*` roles |
| UI | Client implementation link was broken; navigation did not distinguish mock auth or future Admin Console | Fixed the link, added an implementation-accurate Mermaid route map, and clearly marked real Keycloak/API/Admin Console work as future |
| Workflow | Idea-to-documentation methodology and project roles were scattered | Added the full workflow and Clio/Deep/Brio/Stitch/Elito responsibilities to README and CONTRIBUTING |
| Indexing | OKF omitted ADR-0008/0009 and reported completed capabilities as wholly planned | Added decisions and capability-specific implemented-versus-pending status |
| Blueprint | Blueprint declared itself the primary current truth despite later ADRs | Labeled it historical intent and made later accepted decisions/current reports authoritative where evolution occurred |

## Modified documents

- `README.md`
- `CONTRIBUTING.md`
- `client/README.md`
- `server/README.md`
- `server/contracts/README.md`
- `server/platform/README.md`
- all four service READMEs
- `docs/roadmap/Arbitrier-Roadmap-v1.md`
- `docs/okf/index.md`, `docs/okf/UC-01-corporate-bulk-order.md`, and `docs/okf/project-blueprint.md`
- `docs/rf/RF-0001-corporate-bulk-order.md` and `docs/rf/RF-UC-01-corporate-bulk-order.md`
- `docs/rnf/RNF-0001-technical-baseline.md` and `docs/rnf/RNF-UC-01-saga-runtime.md`
- ADR-0002, ADR-0003, and ADR-0004
- `docs/ui/ux_strategy_navigation_map.md`
- TC-UC-01-003 through TC-UC-01-006 status/terminology notes
- completed primary task status headers
- this audit report

## Remaining documentation debt

1. The persistence work has a task specification and implementation in source but no dedicated `ARB-019-persistence-adapters.md` implementation report or Deep review report. A future documentation-only backfill should record exact mappings, Testcontainers coverage, and optimistic-lock exception behavior from the completed slice.
2. Seed documents under `docs/okf/seeds/` intentionally preserve the original discovery model, including the old in-saga customer wait. They should remain clearly treated as inputs, not current requirements; converting them into an archived folder would reduce accidental citation.
3. Several historical implementation reports contain open questions later resolved by subsequent slices. They remain valid historical records, but a generated decision-resolution index would make resolution lineage easier to follow.
4. Customer-decision Avro schemas and legacy `AWAITING_CUSTOMER_DECISION` enum values remain in code/contracts for compatibility even though they are not on the active UC-01 path. Removal or formal deprecation requires a code/contract task, outside this audit.
5. Exact Kafka topic names, partitioning keys, runtime timeout/backoff, outbox/inbox structures, dashboard projections, and deployed observability remain genuine open decisions tied to planned roadmap work.
6. No current screenshots are checked into the main Customer Portal documentation. The route map and implementation report are accurate, but stable release screenshots should be added after the UI is connected to real APIs to avoid rapidly stale imagery.

## Recommendations

- Backfill and Deep-review the ARB-019 implementation report before ARB-020 starts.
- Add a small documentation link checker to CI when ARB-030 is implemented.
- Add `Superseded by`/`Resolved by` metadata to historical open questions instead of deleting their evidence.
- Create one runtime integration document during ARB-022/024 showing topics, consumer groups, retry scheduling, transaction/outbox boundaries, and observability signals.
- Capture Customer Portal screenshots only at an integrated milestone and label mock versus live data visibly.

## Validation

Validation for this documentation-only task includes repository-local Markdown link/path checking, stale-term searches across active documentation, roadmap/task-status comparison, and `git diff --check`. No application build was required because no executable source or configuration changed.
