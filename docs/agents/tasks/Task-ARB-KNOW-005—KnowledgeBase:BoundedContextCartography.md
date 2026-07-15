Task: ARB-KNOW-005 — Knowledge Base: Bounded Context Cartography

Status:
[PLANNED]

Owner:
Elito

Role:
Knowledge Curator

Timebox:
Target: 30–45 minutes
Hard stop: 45 minutes

Apply:

.claude/skills/knowledge-curator/SKILL.md
.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

If interrupted:

Create:

docs/agents/checkpoints/CHECKPOINT-ARB-KNOW-005.md

and stop.

--------------------------------------------------

Context

The Knowledge Base structure now exists.

The next objective is to begin populating durable knowledge.

The first durable concepts are the project's bounded contexts.

This task documents the conceptual boundaries of the system.

It does not document implementation classes.

It does not document database schemas.

It does not document sequence flows.

--------------------------------------------------

Goal

Create the first durable knowledge pages describing every bounded context
present in Arbitrier.

These pages should help a new engineer understand:

- why the bounded context exists;
- what it owns;
- what it does not own;
- how it collaborates with other contexts;
- where authoritative implementation lives.

--------------------------------------------------

Create

knowledge/domain/

    order-service.md

    inventory-service.md

    credit-service.md

    orchestrator-service.md

    platform.md

Update:

knowledge/domain/index.md

--------------------------------------------------

Each page should include

# Purpose

What responsibility this bounded context owns.

# Responsibilities

Primary business responsibilities.

# Does NOT Own

Explicitly document what belongs elsewhere.

# Main Concepts

Aggregates

Value Objects

Domain Services

Commands

Events

(if applicable)

Describe concepts, not implementation classes.

# Collaborates With

Other bounded contexts.

Direction of collaboration.

No implementation details.

# Source of Truth

Reference authoritative artifacts such as:

- ADRs
- RF/RNF
- contracts
- implementation packages

The knowledge page summarizes.

It does not replace authority.

# Future Knowledge

List knowledge pages that will eventually belong here.

Examples:

Events

Tables

Workflows

--------------------------------------------------

Knowledge Principles

Write durable knowledge.

Avoid:

- package names
- class names
- Spring annotations
- KafkaTemplate
- JPA details
- implementation mechanics

The page should remain valid after major refactoring.

--------------------------------------------------

Validation

Verify:

- all links resolve;
- every bounded context is reachable from knowledge/domain/index.md;
- terminology is consistent with the ubiquitous language;
- no duplicated authority;
- no implementation-specific documentation leaked into the pages.

--------------------------------------------------

Documentation

Create:

docs/agents/reports/ARB-KNOW-005-bounded-context-cartography.md

using the Engineering Reporting protocol.

--------------------------------------------------

Out of Scope

Do NOT:

- create Mermaid diagrams yet;
- document database tables;
- document workflows;
- document sequence diagrams;
- document runtime;
- redesign architecture;
- modify application code.

--------------------------------------------------

Acceptance Criteria

✓ Every bounded context has an initial durable knowledge page.

✓ Domain index links to every page.

✓ Responsibilities and boundaries are explicit.

✓ Source of Truth is identified.

✓ Knowledge remains implementation-independent.

✓ Completion report created.

Do not begin diagram creation.