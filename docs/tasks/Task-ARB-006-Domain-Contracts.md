Task: ARB-006 — Domain Contracts

Status:
[PLANNED]

Owner:
Clio

Context:
ARB-005 Domain Model is DONE with Deep PASS.
The pure Java domain model now defines the UC-01 vocabulary.
ARB-006 translates that domain vocabulary into external message contracts.

Goal:
Create Avro domain event and command contracts for UC-01 without implementing Kafka producers/consumers.

Module in scope:
- server/contracts

Documentation in scope:
- docs/implementation/ARB-006-domain-contracts.md
- docs/adr/ADR-0004-avro-contracts-and-schema-registry.md may be updated only if needed
- docs/okf/index.md may be updated only if needed

Out of scope:
- No KafkaTemplate usage.
- No Kafka consumers.
- No Kafka producers.
- No service adapters.
- No REST endpoints.
- No JPA.
- No database migrations.
- No application services.
- No business logic changes in domain models.
- No native image RuntimeHints unless absolutely required by generated Avro classes.

Contract design principles:
- Contracts derive from ARB-005 domain model.
- Contracts must not introduce new business behavior.
- Contracts are integration language, not shared domain objects.
- Service domains remain decoupled from generated Avro classes.
- Cross-bounded-context references use string IDs in contracts.
- Every event/command must include metadata:
    - messageId
    - correlationId
    - causationId
    - occurredAt
    - schemaVersion

Suggested package/namespace:
- com.arbitrier.contracts.order
- com.arbitrier.contracts.inventory
- com.arbitrier.contracts.credit
- com.arbitrier.contracts.orchestrator
- com.arbitrier.contracts.common

Required common schemas:
- MessageMetadata
    - messageId: string
    - correlationId: string
    - causationId: ["null", "string"]
    - occurredAt: string
    - schemaVersion: string

- MoneyAmount
    - amount: string
    - currency: string

- OrderLineContract
    - sku: string
    - quantity: int

Required order contracts:
- OrderCreated
    - metadata
    - orderId
    - customerId
    - submittedByUserId
    - lines
    - requestedTotal

- OrderConfirmed
    - metadata
    - orderId
    - customerId
    - confirmedTotal

- OrderPartiallyConfirmed
    - metadata
    - orderId
    - customerId
    - confirmedLines
    - backorderLines
    - confirmedTotal

- OrderCancelled
    - metadata
    - orderId
    - customerId
    - cancellationReason

Required inventory contracts:
- ReserveStockRequested
    - metadata
    - orderId
    - reservationId
    - lines

- StockReserved
    - metadata
    - orderId
    - reservationId
    - warehouseId
    - reservedLines

- StockPartiallyReserved
    - metadata
    - orderId
    - reservationId
    - warehouseId
    - reservedLines
    - backorderLines

- StockRejected
    - metadata
    - orderId
    - reservationId
    - rejectionReason

- ReleaseStockRequested
    - metadata
    - orderId
    - reservationId
    - reason

- StockReleased
    - metadata
    - orderId
    - reservationId

Required credit contracts:
- ReserveCreditRequested
    - metadata
    - orderId
    - creditReservationId
    - customerId
    - amount

- CreditApproved
    - metadata
    - orderId
    - creditReservationId
    - approvedAmount

- CreditRejected
    - metadata
    - orderId
    - creditReservationId
    - rejectionReason

- ReleaseCreditRequested
    - metadata
    - orderId
    - creditReservationId
    - reason

- CreditReleased
    - metadata
    - orderId
    - creditReservationId

Required orchestrator/customer decision contracts:
- CustomerDecisionRequested
    - metadata
    - orderId
    - sagaId
    - availableLines
    - backorderLines

- CustomerDecisionSubmitted
    - metadata
    - orderId
    - sagaId
    - decision

- SagaCompleted
    - metadata
    - orderId
    - sagaId

- SagaCancelled
    - metadata
    - orderId
    - sagaId
    - cancellationReason

- SagaCompensationFailed
    - metadata
    - orderId
    - sagaId
    - failedAction
    - reason

Schema requirements:
- Use Avro .avsc files under server/contracts/src/main/avro.
- Use explicit namespaces.
- Avoid Java-specific assumptions in schemas.
- Use string for decimal amounts in v1 to avoid logical type/native-image complexity unless already agreed.
- Use string for timestamps in ISO-8601 format in v1.
- Use enums where stable:
    - CancellationReason
    - CustomerDecision
    - CompensationAction
- Use string for rejectionReason/reason unless domain enum already exists.
- Include doc fields in schemas where useful.
- Keep schemas backward-compatibility friendly.

Build requirements:
- Activate Avro Maven plugin only in server/contracts if needed.
- Generated sources must remain inside contracts module.
- Do not make service modules depend on generated Avro classes yet.
- Add schema parsing/compatibility tests.
- Tests must run without Kafka, Postgres, Keycloak, or Docker.

Native Image constraints:
- Document whether generated Avro classes introduce reflection/resource concerns.
- Do not add runtime hints unless generated classes require them immediately.
- If uncertain, mark OPEN QUESTION.

Testing:
- Schema files parse.
- Required schemas exist.
- Required fields exist.
- Metadata field exists in every event/command.
- Enums contain expected values.
- No schema introduces tenantId in v1.
- No schema imports service domain classes.
- Optional: generated classes compile if Avro plugin is activated.

Documentation:
Create docs/implementation/ARB-006-domain-contracts.md with:
- list of schemas created
- metadata convention
- naming convention
- what is intentionally not implemented
- native image considerations
- open questions

Acceptance Criteria:
- All required .avsc files exist or omitted only with explicit justification.
- Contracts module compiles.
- Schema tests pass.
- No service module uses generated Avro classes yet.
- No Kafka runtime code exists.
- No business domain model changes are introduced.
- Native Image compatibility is considered and documented.
- ARB-006 is ready for Deep review.

After completion:
- Report created files.
- Report build/test status.
- Report open questions.
- Do not start ARB-007.