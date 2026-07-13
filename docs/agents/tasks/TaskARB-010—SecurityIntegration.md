Task: ARB-010 — Security Integration

Status:
[COMPLETE]

Owner:
Clio

Context:
ARB-007 Order Service is DONE, but submittedByUserId is temporarily accepted from the request body.
ARB-009 Observability Foundation is DONE.
Now order-service must derive the authenticated user from the JWT instead of trusting request input.

Goal:
Integrate Spring Security Resource Server basics for order-service and remove trusted user identity from the request payload.

Scope:
- server/platform
- server/order-service
- docs/implementation/ARB-010-security-integration.md

In scope:
1. Platform security model:
    - AuthenticatedUser value object
    - AuthenticatedUserProvider interface
    - UserId extraction convention from JWT subject
    - role/authority helper if minimal

2. order-service security:
    - Spring Security Resource Server dependency/config
    - JWT authentication enabled
    - POST /api/orders requires authenticated user
    - controller derives submittedByUserId from authenticated JWT subject
    - request body no longer contains submittedByUserId

3. Customer authorization:
    - CustomerAccessPort in order-service application outbound ports
    - application service validates canSubmitOrder(userId, customerId)
    - if false, reject with authorization problem
    - test adapter allows/denies access

4. Error behavior:
    - missing/invalid token → 401
    - valid token but no customer access → 403
    - validation failure → 400
    - business/application problem → existing mapped response

5. Tests:
    - authenticated user can submit order for allowed customer
    - request cannot spoof submittedByUserId
    - missing JWT returns 401
    - valid JWT but unauthorized customer returns 403
    - submittedByUserId is no longer accepted in request payload
    - existing happy-path application service tests still pass

Out of scope:
- No Keycloak server setup beyond config placeholders.
- No real customer membership database.
- No JPA.
- No Kafka.
- No Avro mapper.
- No gateway/BFF.
- No tenant model.
- No full RBAC matrix.
- No UI login flow.
- No production secrets.

Implementation guidance:
- Keep domain model security-agnostic.
- Security belongs in adapter/platform/application boundary.
- Do not inject Spring Security types into domain.
- Prefer mapping JWT → platform AuthenticatedUser → application command.
- CustomerAccessPort belongs to application outbound port, not domain.
- Use mock/test implementations for customer access.

Native Image:
- Document Spring Security / JWT native image considerations.
- Do not add RuntimeHints unless required.
- Avoid reflection-heavy custom converters.

Documentation:
Create:
- docs/implementation/ARB-010-security-integration.md

Update:
- docs/implementation/ARB-007-order-service-application-slice.md to close submittedByUserId OPEN QUESTION
- server/order-service/README.md
- server/platform/README.md
- AGENTS.md / CLAUDE.md if needed

Acceptance Criteria:
- order-service compiles.
- tests pass without real Keycloak.
- submittedByUserId removed from CreateOrderRequest.
- UserId comes from JWT subject.
- CustomerAccessPort exists and is enforced.
- 401/403 behavior is tested.
- Domain remains free of Spring Security.
- No Kafka/JPA/Avro infrastructure introduced.
- ARB-010 ready for Deep review.

After completion:
- Report created files.
- Report tests run.
- Report open questions.
- Do not start ARB-011.
