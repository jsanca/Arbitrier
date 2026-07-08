Task: ARB-006-FIX-001 — Resolve Deep Review Warnings

Status:
[PLANNED]

Owner:
Clio

Context:
Deep reviewed ARB-006 with PASS WITH WARNINGS.
No blockers were found. Contracts are structurally correct.

Goal:
Apply only the documentation cleanups requested by Deep.

Changes:

1. Update docs/adr/ADR-0004-avro-contracts-and-schema-registry.md

- Replace stale ARB-002 wording.
- State that ARB-006 defined 26 Avro schemas covering UC-01 events and commands.
- Close or remove the resolved OPEN QUESTION:
  "Exact event and command schemas."

2. Update docs/implementation/ARB-006-domain-contracts.md

Add an OPEN QUESTION or design note for:

- OrderCreated.requestedTotal exists in the Avro contract.
- Order domain currently does not carry requestedTotal.
- Source of truth is not yet finalized.
- Candidate sources:
  - application layer computes it from catalog/pricing data;
  - order-service stores it later when pricing is modeled;
  - catalog/pricing service becomes source of truth in a future phase.

Do NOT:
- Change Avro schemas.
- Change generated classes.
- Change domain model.
- Add Kafka producers/consumers.
- Add topic names.
- Add RuntimeHints.
- Start ARB-007.

Acceptance Criteria:
- ADR-0004 no longer contains stale ARB-002 language.
- ADR-0004 reflects ARB-006 schemas exist.
- Implementation note documents requestedTotal source-of-truth uncertainty.
- No code changes.
- No schema changes.
- ARB-006 remains ready to mark [DONE].