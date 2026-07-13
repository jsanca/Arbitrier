# Arbitrier

Arbitrier is a Java 25 / Spring Boot 4.1 reference system for B2B bulk-order orchestration. It models order submission, global inventory allocation, credit reservation, compensation, persistence, and reliable outbound messaging across independently owned bounded contexts.

The repository currently contains the domain/application core, JPA/Flyway persistence, outbox/inbox foundation, local PostgreSQL/Kafka/Keycloak runtime stack, and a mock-backed React Customer Portal. Kafka runtime adapters remain planned; ARB-022.1 is an in-progress outbound message routing foundation, not a completed Kafka implementation.

## Architecture at a glance

The buyer performs advisory global availability negotiation before submission. Inventory owns warehouse selection and may allocate across locations. Once submitted, the Saga coordinates authoritative inventory and credit work; failures follow explicit compensation paths.

| Module | Responsibility | Current state |
|---|---|---|
| `order-service` | Order lifecycle and authenticated entry | REST, JPA/Flyway, transactional outbox integration |
| `inventory-service` | Global availability and reservations | Multi-warehouse domain, JPA/Flyway, outbox integration |
| `credit-service` | Credit reservation and release | JPA/Flyway and outbox integration; external credit source pending |
| `orchestrator-service` | Saga transitions and compensation | JPA/Flyway, retry decisions, outbox integration; scheduler/runtime consumers pending |
| `contracts` | Shared Avro contracts | Generated types and schemas; production serializer/runtime consumers pending |
| `platform` | Cross-cutting primitives | Correlation, validation, errors, outbox/inbox model, Spring infrastructure |
| `client` | Customer Portal | React 19 localStorage/mock prototype; real API and Keycloak integration pending |

## Quick start

Prerequisites: Java 25, Maven, Node.js 20+, and Docker Compose v2.

```bash
infra/docker/start.sh
infra/docker/health.sh

mvn -B verify --no-transfer-progress

cd client
npm ci
npm run dev
```

Local service URLs, development credentials, reset steps, and environment variables are in [the local runtime guide](infra/docker/README.md). The Customer Portal runs at <http://localhost:5173> and uses mock services, so it does not require the backend stack.

## Repository and documentation map

```text
server/                  Maven services, contracts, and platform
client/                  React Customer Portal prototype
infra/docker/            Local runtime and SQL initialization
docs/
  agents/
    tasks/               Execution instructions
    reports/             Implementation and fix history
    reviews/             Review and audit history
    checkpoints/         Incomplete-task operational state
    templates/           Reusable report templates
  engineering/           Shared process, ownership, and curation rules
  implementation/        Reserved canonical capability descriptions
  adr/                   Architecture decisions
  okf/, rf/, rnf/        Product, functional, and non-functional requirements
  roadmap/               Planned and completed roadmap slices
  test-cases/            UC-01 behavior specifications
  ui/                    Customer Portal UX material
ENGINEERING_LOG.md       Navigable index of durable delivery artifacts
```

`docs/implementation/` currently contains no active Markdown artifacts after the manual migration. Historical delivery evidence is under `docs/agents/`; current system behavior belongs in this README, service READMEs, ADRs, requirements, and runtime documentation.

Start with the [OKF index](docs/okf/index.md), [roadmap](docs/roadmap/Arbitrier-Roadmap-v1.md), and [Engineering Log](ENGINEERING_LOG.md). For process and artifact ownership, see [documentation ownership](docs/engineering/documentation-ownership.md) and the [Knowledge Curator role](docs/engineering/knowledge-curator.md).

## Engineering workflow

```text
Idea → ADR → Task → Implementation → Review → Fix → Done → Documentation
```

Tasks live in `docs/agents/tasks/`; implementation/fix reports in `docs/agents/reports/`; reviews/audits in `docs/agents/reviews/`; and incomplete work is captured in `docs/agents/checkpoints/`. The [engineering-reporting skill](.claude/skills/engineering-reporting/SKILL.md) defines required sections and evidence rules. The execution-timebox policy requires an OPEN checkpoint to be resolved or superseded before a task is reported DONE.

Clio owns backend implementation, Deep independent review, Brio frontend delivery, Stitch visual exploration, and Elito infrastructure plus knowledge curation. The Knowledge Curator reconciles reports, reviews, fixes, and canonical documentation without inventing evidence or rewriting history.

## Status

Completed roadmap foundations include JPA/Flyway persistence (ARB-019/020), the outbox/inbox foundation and cleanup (ARB-021), local runtime (ARB-027), and the Customer Portal prototype. Planned work includes Kafka runtime adapters, Schema Registry serializer finalization, runtime retry policies, dashboard APIs, production portal integration, and delivery infrastructure. See [ENGINEERING_LOG.md](ENGINEERING_LOG.md) for durable artifact status; do not infer completion from an unreviewed report.
