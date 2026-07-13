# ARB-010 — Security Integration — Review

| Field    | Value       |
|----------|-------------|
| Task     | ARB-010     |
| Status   | Implemented |
| Date     | 2026-07-08  |
| Reviewer | Deep        |

## Verdict

**PASS**

---

## Summary

ARB-010 correctly removes the temporary `submittedByUserId` from the REST request body and derives the authenticated user exclusively from the JWT subject through Spring Security Resource Server. Authentication and authorization boundaries are clean: the application layer knows only `String userId` through the command, the domain layer has zero Spring Security awareness, and the `CustomerAccessPort` outbound port cleanly separates authorization logic from business logic. The `ProblemCode.httpStatus()` abstraction keeps the platform exception handler generic. Test coverage is thorough — 7 controller tests and 3 new service-level authorization tests.

---

## 1. Authentication: PASS

| Criterion | Evidence |
|-----------|----------|
| JWT Resource Server correctly configured | `SecurityConfiguration.java` — `oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)))` with stateless sessions, CSRF disabled |
| Authentication derives from JWT subject only | Controller: `authentication.getName()` maps to JWT `sub` claim |
| Request body cannot spoof user identity | `CreateOrderRequest.java` — `submittedByUserId` field removed. Test `request_body_with_extra_field_cannot_spoof_user_id` proves even if body includes `"submittedByUserId": "spoofed-user"`, the controller uses `jwt-user-789` |
| No authentication logic leaks into domain | Domain model (`Order.java`, `OrderLine.java`, etc.) has zero authentication imports. ArchitectureTest enforces domain→no Spring/JPA rule |
| Authentication failures produce HTTP 401 | `SecurityConfiguration` uses `.anyRequest().authenticated()`. Test `unauthenticated_request_returns_401` confirms |

---

## 2. Authorization: PASS

| Criterion | Evidence |
|-----------|----------|
| `CustomerAccessPort` belongs to application layer | Interface in `application/port/outbound/` — correct hexagonal outbound port |
| Authorization evaluated before Order creation | `SubmitCorporateBulkOrderService.java:69-73` — `canSubmitOrder()` checked before `Order.create()` |
| Valid JWT but unauthorized customer returns HTTP 403 | `OrderProblemCode.CUSTOMER_ACCESS_DENIED` declares `httpStatus = 403`. Test `customer_access_denied_returns_403` confirms `ORDER_ACCESS_DENIED` with 403 |
| Domain model does not know Spring Security or JWT | Verified: domain only imports `com.arbitrier.platform.validation.Require` |

---

## 3. Layer Boundaries: PASS

| Criterion | Evidence |
|-----------|----------|
| Platform owns `AuthenticatedUser` abstraction | `platform/security/AuthenticatedUser.java` (record) and `AuthenticatedUserProvider.java` (interface) |
| Controller does not expose Spring Security types beyond adapter | Controller accepts `Authentication` (adapter layer), extracts `String userId = authentication.getName()`, passes only the string into the command. No Spring Security types enter application layer |
| Application layer depends only on platform abstractions | `ApplicationProblemException`, `ProblemCode` from platform |
| Domain remains framework independent | Unchanged from ARB-007 — zero new imports |
| `CustomerAccessPort` is an outbound port, not infrastructure | It's in `application/port/outbound/`. Implementation (`AllowAllCustomerAccessAdapter`) is in `adapter/outbound/customer/` |

---

## 4. HTTP API: PASS

| Status | Scenario | Mechanism |
|--------|----------|-----------|
| 201 | Success | `ResponseEntity.status(HttpStatus.CREATED)` |
| 400 | Bean validation failure | `PlatformExceptionHandler` catches `MethodArgumentNotValidException` / `IllegalArgumentException` |
| 401 | Missing/invalid JWT | Spring Security filter chain — `.anyRequest().authenticated()` |
| 403 | Valid JWT, no customer access | `OrderProblemCode.CUSTOMER_ACCESS_DENIED` → `ProblemCode.httpStatus()` returns 403 |
| 422 | Business rule violation | `ProblemCode.httpStatus()` default (422) |

`submittedByUserId` removed from request body — identity spoofing no longer possible. Validation behavior unchanged.

---

## 5. Platform Evolution: PASS

| Criterion | Evidence |
|-----------|----------|
| `ProblemCode.httpStatus()` is a clean abstraction | `default int httpStatus() { return 422; }` on `ProblemCode` interface. `OrderProblemCode` overrides for 403 |
| `PlatformExceptionHandler` remains generic | Uses `ex.code().httpStatus()` — no code special-cases for 403 vs 422 |
| No hard-coded status mapping reintroduced | The handler is completely generic; status comes from the problem code |
| Safe logging conventions still respected | Handler logs: `code`, `httpStatus`, `message` — no PII. Service logs `customerId` (business identifier) |

---

## 6. Native Image Compatibility: PASS

| Criterion | Evidence |
|-----------|----------|
| Spring Security config is AOT-friendly | `@EnableWebSecurity` + `SecurityFilterChain` bean — processed by Spring AOT at build time |
| No reflection-heavy custom converters | No custom `JwtAuthenticationConverter` — using Spring Security default converter, which is AOT-processed |
| No `RuntimeHints` required yet, or OPEN QUESTION documented | OPEN QUESTION: `nimbus-jose-jwt` native image verification needed when native build is attempted. Consistent with ADR-0007 |
| Nimbus JWT considerations documented | Implementation note (lines 104-107) references ADR-0007 and the need to validate `NimbusJwtDecoder` reflection hints |

---

## 7. Tests: PASS

**Controller tests** (`SubmitCorporateBulkOrderControllerTest.java` — 7 tests):

| Test | Coverage |
|------|----------|
| `unauthenticated_request_returns_401` | Missing/expired JWT → 401 |
| `authenticated_request_returns_201` | Valid JWT, success path |
| `submitted_by_user_id_comes_from_jwt_subject_not_request_body` | JWT subject extraction from `jwt()` post-processor |
| `request_body_with_extra_field_cannot_spoof_user_id` | Spoofed `submittedByUserId` in body is ignored; JWT subject used |
| `customer_access_denied_returns_403` | Valid JWT, no customer access → 403 `ORDER_ACCESS_DENIED` |
| `missing_customer_id_returns_400` | Validation: missing `customerId` |
| `empty_lines_returns_400` | Validation: empty lines |

**Service tests** (`SubmitCorporateBulkOrderServiceTest.java` — 17 tests):

| Tests | Coverage |
|-------|----------|
| 6 happy path | Creation, repo save, retrieval, event publish, event fields |
| 3 customer access (`DENY_ALL`) | Throws `ApplicationProblemException`, maps to 403, does not save/publish |
| 8 command validation | Null/blank customerId, userId, empty lines, invalid SKU, zero/negative quantity |

All tests run without Keycloak, Docker, Kafka, Postgres, or Schema Registry. Controller tests use `spring-security-test`'s `jwt()` post-processor and mock `JwtDecoder` from `OrderServiceTestConfiguration`.

---

## 8. Documentation: PASS

| Criterion | Evidence |
|-----------|----------|
| Implementation note accurately reflects implementation | All 12 files listed (4 platform, 6 main, 2 test) are present. Architecture diagram matches. Test counts match (7 controller, 17 service). |
| ARB-007 `submittedByUserId` OPEN QUESTION is closed | Explicit "Resolved Open Question" section documents that `submittedByUserId` is removed and JWT subject is the sole identity source |
| Remaining OPEN QUESTIONS are valid | 1) Real `CustomerAccessPort` implementation. 2) JWT `sub` format alignment with Keycloak UUIDs. 3) Actuator endpoint security restriction. 4) `nimbus-jose-jwt` native image compatibility |
| Native image notes consistent with ADR-0007 | References ADR-0007 and the nimbus verification need. Documents Spring AOT handles `@EnableWebSecurity`/`SecurityFilterChain` |

---

## Security Concerns: None

- `submittedByUserId` removed from request body — identity spoofing is structurally impossible
- JWT validated at the filter level before controller is reached
- Customer authorization is evaluated before any business logic (Order creation, persistence, event publishing)
- Safe logging: `customerId` is a business identifier, not PII

---

## Architecture Concerns

**Regarding the special architectural question — `authentication.getName()` vs `AuthenticatedUserProvider.currentUser()`:**

The controller currently does `authentication.getName()` at `SubmitCorporateBulkOrderController.java:56`. This is an idiomatic Spring Security pattern and is architecturally sound:

- The `Authentication` parameter stays entirely within the REST adapter. Only the `String userId` crosses into the application command.
- No Spring Security types enter the application or domain layers.
- The `AuthenticatedUserProvider` interface exists in `platform/security/` for non-REST contexts (Kafka consumers, scheduled tasks) where `SecurityContextHolder` access is needed outside of a controller method parameter.

Introducing `AuthenticatedUserProvider.currentUser()` would add an indirection layer that swaps one Spring Security import (`Authentication`) for another (`SecurityContextHolder` via a provider implementation). For REST controllers, the parameter injection approach is cleaner because the dependency is explicit in the method signature rather than hidden in the method body.

**Recommendation:** The current design is correct for this slice. No change needed.

---

## Evidence

| File | Role |
|------|------|
| `platform/security/AuthenticatedUser.java` | Framework-agnostic principal record |
| `platform/security/AuthenticatedUserProvider.java` | Interface for resolving current user |
| `platform/error/ProblemCode.java` | Added `default int httpStatus()` |
| `platform/web/PlatformExceptionHandler.java` | Uses `ex.code().httpStatus()` |
| `order-service/config/SecurityConfiguration.java` | JWT resource server filter chain |
| `order-service/config/OrderServiceConfiguration.java` | Wires `CustomerAccessPort` bean |
| `order-service/application/OrderProblemCode.java` | `CUSTOMER_ACCESS_DENIED` with httpStatus=403 |
| `order-service/application/port/outbound/CustomerAccessPort.java` | Authorization outbound port |
| `order-service/adapter/outbound/customer/AllowAllCustomerAccessAdapter.java` | Permissive placeholder adapter |
| `order-service/adapter/inbound/rest/SubmitCorporateBulkOrderController.java` | Uses `authentication.getName()` |
| `order-service/adapter/inbound/rest/CreateOrderRequest.java` | `submittedByUserId` removed |
| `order-service/application/service/SubmitCorporateBulkOrderService.java` | Authorization check before Order.create() |
| `order-service/test/.../adapter/inbound/rest/SubmitCorporateBulkOrderControllerTest.java` | 7 controller tests |
| `order-service/test/.../application/service/SubmitCorporateBulkOrderServiceTest.java` | 17 service tests (3 new authorization) |
| `order-service/test/.../integration/OrderServiceTestConfiguration.java` | Mock `JwtDecoder` for tests |

---

## Decision

**ARB-010 may be marked [DONE].** No blockers, no warnings, no code changes needed. The security integration is cleanly implemented with proper layer boundaries, thorough test coverage, and correct HTTP status mapping.
