Task: ARB-KNOW-004 — Initial Knowledge Base

Status:
[PLANNED]

Owner:
Elito

Role:
Knowledge Curator

Apply:

.claude/skills/knowledge-curator/SKILL.md

.claude/skills/execution-timebox/SKILL.md

.claude/skills/engineering-reporting/SKILL.md

--------------------------------------------------

Context

The Knowledge Curator skill is now available.

This task validates the skill by creating the first iteration of the project's
canonical Knowledge Base.

The objective is not completeness.

The objective is to establish the structure, ownership model and navigation
that future knowledge will follow.

--------------------------------------------------

Goal

Create the initial Knowledge Base for Arbitrier.

The Knowledge Base should organize durable project knowledge.

It must not duplicate implementation artifacts.

Instead it should curate and connect the project's authoritative sources.

--------------------------------------------------

Repository Layout

Create:

knowledge/

    index.md

    architecture/
        index.md

    domain/
        index.md

    contracts/
        index.md

    data/
        index.md

    operations/
        index.md

    glossary/
        index.md

Create only the structure and initial navigation.

Do not attempt to exhaustively document the system.

--------------------------------------------------

knowledge/index.md

This page is the entry point into the project's knowledge.

It should explain:

- purpose of the Knowledge Base
- difference between README, docs and knowledge
- durable knowledge philosophy
- source-of-truth principle
- how to navigate the knowledge sections

Include links to every section.

--------------------------------------------------

Section Indexes

Each section should contain an index page describing:

Purpose

Scope

Typical contents

Authoritative sources

Navigation to future pages

The pages should remain intentionally lightweight.

--------------------------------------------------

Source of Truth

Where appropriate, identify the authoritative artifacts.

Examples include:

- ADRs

- RF / RNF

- contracts

- schemas

- implementation

- ENGINEERING_LOG

Knowledge pages should reference authority.

They must not replace authority.

--------------------------------------------------

Do NOT

Do not document:

every table

every event

every workflow

every contract

Those will be added incrementally.

--------------------------------------------------

Consistency

Follow the Knowledge Curator principles.

Avoid duplication.

Prefer navigation over repetition.

Prefer durable concepts over implementation details.

--------------------------------------------------

Validation

Verify:

- repository links
- navigation
- index consistency
- ownership consistency
- no duplicated authority

--------------------------------------------------

Documentation

Create:

docs/agents/reports/ARB-KNOW-004-initial-knowledge-base.md

using the Engineering Reporting protocol.

--------------------------------------------------

Acceptance

✓ knowledge/ hierarchy exists.

✓ Root knowledge index exists.

✓ Section indexes exist.

✓ Navigation is coherent.

✓ Knowledge philosophy documented.

✓ Sources of truth identified.

✓ No implementation duplication.

Do not populate detailed knowledge pages yet.