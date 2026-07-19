# ARB-ADR-0010 — External API Entry Point and Orchestrator Responsibilities

## Execution Header

* **Task:** ARB-ADR-0010 — External API Entry Point and Orchestrator Responsibilities
* **Status:** PLANNED
* **Owner:** Elito
* **Role:** Architecture Documentation
* **Target:** 20–30 minutes
* **Hard stop:** 45 minutes
* **Skills:**
  * Apply `.claude/skills/architecture-decision-records/SKILL.md`
  * Apply `.claude/skills/engineering-reporting/SKILL.md`
  * Apply `.claude/skills/execution-timebox/SKILL.md`
* **Commits:** Do not commit changes.

## Objective

Record the planned UC-01 decision that Order Service owns the external Order-creation command API and that the Orchestrator owns the distributed workflow after Order acceptance. The ADR must explicitly reject treating the Orchestrator as an API Gateway or BFF while deferring those patterns as future architectural options.

## Scope

* Create ADR-0010 under `docs/adr/`.
* Capture the UI, Order, Orchestrator, Inventory, and Credit responsibilities.
* Include the canonical `UI → Order Service → OrderSubmitted → Orchestrator → Inventory → Credit` sequence.
* Document the accepted Order-Service-first and rejected Orchestrator-first alternatives.
* Define when an API Gateway or BFF should be reconsidered.
* Create the required implementation report and add durable records to `ENGINEERING_LOG.md`.

## Out of Scope

* Production code and configuration.
* Tests.
* HTTP endpoint implementation or changes.
* Messaging, persistence, security, and API-versioning implementation.
* Commits.
