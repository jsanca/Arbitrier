Task: ARB-023.6 — Vertical Integration Proof

Status:
[DONE]

Owner:
Clio

Role:
Implementation / Integration Validation

Timebox:
Target: 20–30 minutes
Hard stop: 45 minutes

Apply:

.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

Do not commit.

Context

ARB-023 now contains the complete corporate bulk-order submission path:

- REST request mapping;
- authenticated user extraction;
- customer authorization;
- duplicate-SKU normalization;
- quantity validation;
- Inventory availability verification over gRPC;
- Order creation in PENDING state;
- Order persistence;
- transactional Outbox persistence;
- accepted HTTP response;
- protocol failure mapping.

ARB-023.6 must provide the vertical integration proof for this complete slice.

This is primarily a proof task.

Do not add production behavior unless a real integration defect is discovered.

Do not expand into Kafka publication or Orchestrator execution.

----------------------------------------------------------------------
1. Prove the successful vertical path
----------------------------------------------------------------------

Add or refine an integration test that exercises the production path from the
inbound REST boundary through the real application and outbound adapters.

Target path:

HTTP request
↓
SubmitCorporateBulkOrderController
↓
REST mapper
↓
SubmitCorporateBulkOrderService
↓
CustomerAccessPort
↓
GrpcInventoryAvailabilityAdapter
↓
Inventory gRPC server
↓
OrderRepository
↓
OutboxRepository
↓
HTTP accepted response

The proof must use production Spring wiring wherever practical.

Do not invoke SubmitCorporateBulkOrderService directly as the only proof.

----------------------------------------------------------------------
2. Successful scenario
----------------------------------------------------------------------

Prove a valid corporate bulk-order submission where:

- the authenticated user is authorized for the customer;
- duplicate request SKUs are present;
- duplicate quantities are normalized and summed;
- Inventory reports sufficient stock;
- the HTTP request succeeds;
- exactly one Order is persisted;
- the persisted Order is PENDING;
- persisted lines contain normalized quantities;
- exactly one Outbox event is persisted;
- the Outbox event type represents OrderCreated;
- the Outbox payload contains the same normalized quantities;
- the HTTP result orderId matches the persisted Order;
- the HTTP result status matches the persisted Order state.

Example input:

SKU-A quantity 3
SKU-B quantity 1
SKU-A quantity 2

Expected persisted and event representation:

SKU-A quantity 5
SKU-B quantity 1

----------------------------------------------------------------------
3. Business rejection scenario
----------------------------------------------------------------------

Prove insufficient inventory through the same REST-to-gRPC path.

Expected:

- business error ORDER_ITEMS_UNAVAILABLE;
- expected HTTP status;
- no Order persisted;
- no Outbox event persisted.

Do not replace the real gRPC Inventory path with an application-layer stub for
this proof if the current integration infrastructure supports the real adapter.

----------------------------------------------------------------------
4. Protocol failure scenario
----------------------------------------------------------------------

Prove at least one malformed Inventory response through the production outbound
adapter, preferably:

- missing requested SKU; or
- empty response for a non-empty request.

Expected:

- InventoryAvailabilityProtocolException is classified as
  INVENTORY_PROTOCOL_ERROR;
- HTTP 502 Bad Gateway;
- no Order persisted;
- no Outbox event persisted.

If the real protobuf server cannot represent the malformed scenario naturally,
use the narrowest adapter-boundary test fixture available while preserving the
production adapter validation path.

Document any test seam used.

----------------------------------------------------------------------
5. Authorization rejection scenario
----------------------------------------------------------------------

Prove an authenticated but unauthorized user cannot submit for the customer.

Expected:

- CUSTOMER_ACCESS_DENIED;
- expected HTTP status;
- Inventory is not called;
- no Order persisted;
- no Outbox event persisted.

Use the actual inbound security/user-resolution mechanism already established
for this module where practical.

Do not redesign authorization.

----------------------------------------------------------------------
6. Invalid request scenario
----------------------------------------------------------------------

Prove at least:

- quantity zero rejected;
- negative quantity rejected.

Expected:

- HTTP 400 or the established request-validation response;
- application use case is not executed;
- no Order persisted;
- no Outbox event persisted.

This proof should begin at the REST boundary so Bean Validation is exercised.

----------------------------------------------------------------------
7. Transactional consistency
----------------------------------------------------------------------

ARB-023.5 already proves rollback with a focused JPA integration test.

Do not duplicate that test unnecessarily.

Reference the existing rollback proof in the report and ensure the vertical test
does not weaken or bypass the same transactional repositories.

----------------------------------------------------------------------
8. Test isolation
----------------------------------------------------------------------

Each integration scenario must be independent.

Ensure:

- database state is reset or uniquely scoped;
- Inventory fixtures do not leak between tests;
- security context is reset;
- no test depends on execution order.

Avoid sleeps and timing-based assertions.

----------------------------------------------------------------------
9. Production changes
----------------------------------------------------------------------

If the vertical proof reveals a production defect:

- apply the smallest focused fix;
- add a regression assertion;
- document the defect and fix.

Do not perform opportunistic refactoring.

Do not rename public contracts unless required for correctness.

----------------------------------------------------------------------
10. Validation
----------------------------------------------------------------------

Run the relevant module suite, preferably:

mvn -B test --no-transfer-progress \
-pl server/contracts,server/platform,server/inventory-service,server/order-service

If the full command is too broad for iteration, run focused tests first and the
module suite once before completion.

Report:

- exact tests added or modified;
- total tests executed;
- build result;
- any infrastructure limitations.

----------------------------------------------------------------------
Deliverables

- vertical integration test covering successful submission;
- business rejection proof;
- protocol failure proof;
- authorization rejection proof;
- REST validation proof;
- engineering report describing:
    - the production path exercised;
    - where real adapters were used;
    - any test seams;
    - persistence and Outbox evidence;
    - exact HTTP semantics;
    - whether production changes were required.

Do not implement Kafka publication.

Do not invoke the Orchestrator.

Do not begin ARB-024.

Do not commit.

If incomplete at the hard stop, create:

docs/agents/checkpoints/CHECKPOINT-ARB-023.6.md

and stop.
