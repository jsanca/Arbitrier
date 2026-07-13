Task: ARB-022.1-FIX-001 — Outbound Message Model Review Fixes

Status:
[PLANNED]

Owner:
Clio

Role:
Implementation

Timebox:
Target: 15–20 minutes

Hard stop:
45 minutes

Apply:

.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

If the hard stop is reached:

Create:

docs/agents/checkpoints/CHECKPOINT-ARB-022.1-FIX-001.md

and stop.

--------------------------------------------------

Context

ARB-022.1 received a PASS WITH WARNINGS architecture review.

The review confirms the architecture is correct and ready for ARB-022.2.

Only four findings have been accepted for immediate implementation.

This task is intentionally small.

Do not redesign the messaging model.

Do not begin Kafka integration.

Do not introduce new abstractions.

--------------------------------------------------

Review Findings to Address

G1
Documentation wording:
"exactly once" delivery guarantee.

S1
Duplicate entity discovery configuration.

S1b
EntityScan marker classes.

T2
Missing JPA round-trip test for MessageNature.COMMAND.

All remaining review findings are intentionally deferred.

--------------------------------------------------

1. Correct Delivery Guarantee Documentation

Review all documentation introduced by ARB-022.1.

Replace wording that incorrectly implies:

"exactly once delivery"

with terminology matching the actual guarantees.

Documentation should distinguish:

- atomic persistence
- eventual delivery
- at-least-once transport
- idempotent processing

Do not over-explain.

The wording should remain concise.

--------------------------------------------------

2. Remove Duplicate Entity Discovery

Current implementation contains both:

spring.jpa.packages

and

@EntityScan(...)

Choose @EntityScan as the canonical mechanism.

Remove the duplicated
spring.jpa.packages

configuration from every affected service.

Ensure the build and tests continue to pass.

--------------------------------------------------

3. Improve EntityScan Marker Classes

Review every PersistenceConfiguration.

Where a repository interface is currently used only as a package marker,
replace it with the corresponding entity class.

Example:

Replace:

SpringDataOrderRepository.class

with:

OrderEntity.class

(or the equivalent entity for each service).

The marker should semantically represent entity scanning.

Do not change scanning scope.

--------------------------------------------------

4. Add COMMAND Round-trip Test

Extend the existing JPA adapter tests.

Create a persistence round-trip using:

MessageNature.COMMAND

Verify:

- save
- retrieval
- recovered MessageNature

No additional routing tests are required.

No migration tests are required.

--------------------------------------------------

5. Validation

Run the smallest reliable validation.

Expected:

- project builds
- affected tests pass
- documentation links remain valid
- git diff --check passes

Do not run unnecessary full-system validation if targeted validation is
sufficient.

--------------------------------------------------

Out of Scope

Do NOT:

- introduce LogicalDestination
- replace String with a value object
- persist routing destinations
- redesign OutboundRoutingStrategy
- modify Kafka adapters
- implement command publishing
- create migration compatibility tests
- modify roadmap
- rewrite review documents

--------------------------------------------------

Acceptance Criteria

✓ Documentation no longer claims exactly-once delivery.

✓ @EntityScan becomes the single entity discovery mechanism.

✓ Repository interfaces are no longer used as EntityScan markers.

✓ COMMAND JPA round-trip test exists.

✓ Build passes.

✓ Targeted tests pass.

✓ Documentation remains consistent.

--------------------------------------------------

Completion Report

Create:

docs/agents/reports/ARB-022.1-FIX-001-outbound-message-model-review-fixes.md

Include:

- review findings addressed
- files modified
- validation executed
- tests executed
- deferred findings (explicitly list those intentionally left for future
  slices)

Do not begin ARB-022.2.