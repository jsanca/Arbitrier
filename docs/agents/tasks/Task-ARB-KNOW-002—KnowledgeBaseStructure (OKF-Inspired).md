Task: ARB-KNOW-002 — Knowledge Base Structure (OKF-Inspired)

Status:
[PLANNED]

Owner:
Elito

Role:
Knowledge Curator

Apply:

.claude/skills/engineering-reporting/SKILL.md

Do not migrate existing documentation yet.

--------------------------------------------------

Context

The project currently separates:

- engineering documentation
- implementation reports
- architecture decisions

We would like to introduce a dedicated knowledge base inspired by the Open
Knowledge Framework (OKF).

The goal is not to copy OKF verbatim, but to adapt its ideas to software
engineering and Domain-Driven Design.

--------------------------------------------------

Goal

Design the canonical structure of the project's Knowledge Base.

This structure should become reusable by future projects and eventually be part
of the DeIngeniumLibri archetype.

--------------------------------------------------

Deliverables

Design a proposed directory structure.

Example inspiration only:

knowledge/
index.md

    datasets/
        index.md

    tables/
        index.md

Do not assume this structure is final.

Evaluate alternatives.

--------------------------------------------------

Evaluate

At minimum consider whether the Knowledge Base should contain sections such as:

- datasets
- schemas
- tables
- events
- commands
- queries
- aggregates
- value objects
- ports
- adapters
- workflows
- APIs
- metrics
- glossary

Recommend which belong in the canonical structure and which should remain
project-specific.

--------------------------------------------------

Indexes

Every folder should define:

index.md

Explain:

- purpose
- navigation
- ownership
- relationships

--------------------------------------------------

Document Template

Design a reusable template for knowledge documents.

Evaluate whether metadata should include fields such as:

- title
- type
- owner
- bounded context
- source of truth
- tags
- status
- last reviewed

Do not over-design.

Keep metadata useful for both humans and AI agents.

--------------------------------------------------

Relationship with Existing Documentation

Clarify the responsibilities of:

docs/engineering/
docs/agents/
knowledge/

Explain what belongs in each location.

The goal is to avoid duplication.

--------------------------------------------------

Knowledge Curator Responsibilities

Define how the Knowledge Curator should maintain the knowledge base.

Examples:

- broken links
- stale documents
- missing knowledge pages
- missing indexes
- orphaned documents
- ownership review

--------------------------------------------------

Future Compatibility

The proposal should be generic enough to work for:

- Arbitrier
- Codex
- myIR
- future projects

Avoid project-specific assumptions.

--------------------------------------------------

Output

Produce:

1. Proposed directory structure.
2. Rationale for each top-level section.
3. Canonical document template.
4. Documentation ownership guidelines.
5. Migration strategy from the current documentation.
6. Recommendation on whether to adopt OKF frontmatter completely, partially,
   or with adaptations.

Do not move files.

Do not modify the repository.

This task is a design proposal only.