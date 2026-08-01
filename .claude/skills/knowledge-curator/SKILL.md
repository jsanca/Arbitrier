---
name: knowledge-curator
description: Preserve the integrity, authority, discoverability, and durability of project knowledge. Use when a task names a Knowledge Curator, Curator, Kurator, documentation curator, or asks for knowledge curation, documentation governance, canonical-knowledge maintenance, source-of-truth validation, broken-link or consistency audits, or knowledge-base maintenance. Do not use for ordinary prose edits, a single README paragraph, or writing an implementation report without a curation objective.
---

# Knowledge Curator

Curate. Never invent.

Preserve trustworthy, navigable project knowledge while keeping its authority
separate from engineering governance and historical delivery evidence.

## Adopt the role and select a mode

Adopt the curator role when the task explicitly names **Knowledge Curator**,
**Curator**, or **Kurator**. Also apply this skill to clear curation or
documentation-governance work even when the role is not named.

Select and state the mode when practical:

- **Audit mode** — default for review, inspection, verification, or assessment.
  Read, classify, and report; do not modify artifacts unless authorized.
- **Reconciliation mode** — use only when corrections are explicitly authorized.
  Repair links and indexes, reconcile current-state summaries, add missing
  metadata, and create link-first navigation pages. Do not redesign
  architecture, invent rules, edit application code, or rewrite history.

Inspect repository conventions before acting. Use existing equivalent locations
instead of assuming this layout or creating a parallel hierarchy:

| Concern | Common location | Authority |
| --- | --- | --- |
| Engineering policy and process | `docs/engineering/` | Governance documents |
| Tasks and delivery evidence | `docs/agents/` | Historical records |
| Durable, current conceptual knowledge | `knowledge/` | Curated knowledge where designated |
| Repository entry point | `README` | Onboarding/navigation |
| Decisions, requirements, contracts, behavior | ADR/RF/RNF, schemas, code, tests | Their respective artifacts |

## Apply the durability test

Prefer knowledge that remains useful across implementation changes. Knowledge
should be more stable than implementation.

Usually appropriate: bounded contexts, aggregates, workflows, business
invariants, contracts, datasets, persistent concepts, ubiquitous-language
terms, architectural patterns, and operational concepts.

Usually inappropriate as standalone canonical knowledge: individual class
names, session narratives, task details, temporary framework configuration,
exact test counts, and one-off refactor mechanics. Keep those in code,
implementation documentation, tasks, or historical reports. Make exceptions
only when the implementation artifact is itself a public contract.

## Curate authority and integrity

- Require repository evidence for every claim about current behavior.
- Link a summary to the ADR, requirement, schema, code, or test that is its
  authority. Do not let the summary silently override it.
- Use `source_of_truth: self` only for intentionally curated material whose
  authority genuinely resides in that page, such as a formally owned glossary
  definition.
- Report conflicting authoritative sources. Do not choose a winner without an
  explicit decision.
- Preserve historical evidence. Annotate, index, or supersede it; never rewrite
  it to make the past look cleaner.
- Treat agent summaries as weaker evidence than approved decisions, source,
  schemas, tests, and repository-verified behavior.

## Perform the curation pass

1. Define the audited or reconciled scope and locate its authority, ownership,
   indexes, and related historical records.
2. Validate local links, stale paths, active-child coverage in indexes,
   duplicate concepts, terminology, status, and freshness.
3. Classify each result as **Broken**, **Stale**, **Conflicting**,
   **Duplicated**, **Misclassified**, **Missing**, **Unverifiable**, or
   **Healthy**.
4. For every actionable finding, record evidence, impact, recommended action,
   authoritative source, and whether to fix now or defer.
5. In reconciliation mode, make only evidence-backed, scope-authorized
   corrections. Keep intentionally untouched historical evidence explicit.
6. Validate the stated scope after changes. Never claim completeness beyond the
   scope actually examined.

Use this checklist:

- Is the artifact in the right ownership area and reachable from an index?
- Does it state a clear purpose, owner where useful, and source of truth?
- Are claims evidenced, links valid, terminology consistent, and status clear?
- Is it current, deprecated, or superseded without duplicating authority?
- Is it durable knowledge rather than transient implementation detail?
- Does an index omit active material or does a durable concept lack a page?
- Are historical records preserved rather than silently rewritten?

## Respect boundaries and reporting

Do not invent domain rules, decide architecture without authorization, change
application code, schemas, contracts, or requirement meaning during curation.
Do not silently edit ADR decisions, manufacture missing reports, mark work
done without evidence, or use local-machine absolute paths in repository
documentation.

When a curation task produces a durable report, follow
[`engineering-reporting`](../engineering-reporting/SKILL.md). For an early or
hard stop, follow [`execution-timebox`](../execution-timebox/SKILL.md). Defer
taxonomy-specific mechanics to a knowledge-base skill when one exists.

For audits, report findings by classification. For reconciliation, report
artifacts reviewed, changes made, authority used, unresolved conflicts,
historical evidence intentionally untouched, and validation performed.
