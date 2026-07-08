# ARB-007 — Order Service Application Slice — Review

| Field    | Value       |
|----------|-------------|
| Task     | ARB-007     |
| Status   | Implemented |
| Date     | 2026-07-07  |
| Reviewer | Deep        |

## Verdict

**PASS WITH WARNINGS**

---

## Summary

ARB-007 delivers a clean, well-architected application slice with disciplined hexagonal boundaries, no infrastructure leakage, and thorough test coverage. The code is native-image compatible and logging is PII-safe. One documentation gap exists around the `submittedByUserId` security concern.

---

## 1. Scope Discipline: PASS

| Concern | Status |
|---------|--------|
| No KafkaTemplate | Not imported; deps commented out in `pom.xml` |
| No Kafka producer/consumer | None |
| No Avro generated class usage | `arbitrier-contracts` on classpath but never imported |
| No JPA entity | None; deps commented out |
| No repository adapter with real persistence | Only `InMemoryOrderRepository` in test |
| No Flyway/Liquibase | None |
| No Keycloak/Spring Security | No deps, no config |
| No pricing/requestedTotal logic | `Money.java` exists in domain but unused; documented as OPEN QUESTION |
| No tenant model | Not present |

---

## 2. Layer Boundaries: PASS

| Constraint | Status |
|------------|--------|
| REST DTOs are not domain objects | `CreateOrderRequest`/`Response` in `adapter/inbound/rest/`; `Order` in `domain/model/` |
| Application commands are not REST DTOs | `SubmitCorporateBulkOrderCommand` in `application/port/inbound/` |
| Domain event is pure Java | `OrderCreatedDomainEvent` is a plain record in `domain/event/`; no library dependency |
| Outbound ports exist | `OrderRepository` and `OrderEventPublisher` in `application/port/outbound/` |
| Application layer does not depend on REST/JPA/Kafka/Avro | Imports only platform + domain + SLF4J |
| Domain does not depend on Spring or application | Imports only `com.arbitrier.platform.validation.Require` |
| ArchitectureTest confirms rules | 3 rules: domain→adapter, application→adapter, domain→Spring/JPA |

---

## 3. Use Case Correctness: PASS

| Behavior | Evidence |
|----------|----------|
| `Order.create(PENDING)` | `SubmitCorporateBulkOrderService.java:56-60` — factory method hardcodes `OrderStatus.PENDING` |
| `OrderRepository.save()` called | `SubmitCorporateBulkOrderService.java:62` |
| `OrderEventPublisher.publish()` called | `SubmitCorporateBulkOrderService.java:74` |
| Result returns `orderId` + `PENDING` | `SubmitCorporateBulkOrderService.java:76` |
| Missing `customerId` rejected | `Require.notBlank` in command compact ctor |
| Missing `submittedByUserId` rejected | `Require.notBlank` in command compact ctor |
| Empty lines rejected | `Require.notEmpty` in command compact ctor |
| Invalid SKU rejected | `Require.notBlank` in line command compact ctor |
| Invalid quantity rejected | `Require.isTrue(quantity > 0)` in line command compact ctor |

---

## 4. Security/Auth Concern: WARNING (documentation gap)

**Observation:** `submittedByUserId` is accepted from the REST request body via `CreateOrderRequest`. This means any API caller can claim any `UserId`.

**Acceptability:** This is acceptable for the pre-security slice. The implementation note correctly states that Keycloak authentication is not yet implemented. However:

**Gap:** The Open Questions section in the implementation note does **not** document the future migration of `submittedByUserId`. It lists 4 questions (requestedTotal, correlationId, endpoint shape, idempotency key) but omits this security concern.

**Expected future model:**
- JWT subject → `UserId` (derived from token, not request body)
- `customerId` remains request/business input
- `CustomerAccessPort.canSubmitOrder(userId, customerId)` validates authorization
- Missing/invalid token → 401
- Valid token but no customer access → 403

**Recommendation:** Add an OPEN QUESTION entry (see Recommendations below) — no code change needed at this time.

---

## 5. Logging/Safe Logging: PASS

| Log statement | Data logged | Sensitivity |
|---------------|-------------|-------------|
| Controller L46: `customerId={}, lines={}` | Business identifier + count | Not PII |
| Controller L59: `orderId={}` | Opaque identifier | Not PII |
| Service L65-66: `orderId={}, customerId={}, lines={}` | Opaque identifiers + count | Not PII |

All domain value types (`UserId`, `CustomerId`, `OrderId`, `Sku`, `Quantity`) hold opaque identifiers — no PII risk. `SafeLoggable`/`SafeRenderable` are omitted by justified choice (the data is non-sensitive business keys per the platform's own `SafeLoggable` javadoc).

---

## 6. Native Image Compatibility: PASS

| Risk | Status |
|------|--------|
| No `Class.forName()` or unrestricted reflection | Not present |
| No runtime-generated CGLIB proxies | Not present |
| No dynamic class loading | Not present |
| `OrderServiceConfiguration` AOT-processable | Standard `@Configuration`/`@Bean` |
| Controller discovery by Spring AOT | Standard `@RestController`/`@RequestMapping` |
| Only native-compatible starters | `web`, `actuator`, `validation` — all AOT-safe |
| No native-hostile libraries | None introduced |

---

## 7. Test Quality: PASS WITH SUGGESTION

| Test suite | Tests | Coverage |
|------------|-------|----------|
| `SubmitCorporateBulkOrderServiceTest` | 10 | Happy path (creation, repo, event, fields) + validation errors |
| `OrderTest` | 10 | Creation invariants, state transitions, guards |
| `ArchitectureTest` | 3 rules | Layer dependency enforcement |
| `OrderServiceApplicationIT` | 1 | Context load |
| **Total** | **24** | All pass without Docker/Kafka/Postgres/Keycloak |

**Suggestions (non-blocking):**
- Add `@WebMvcTest` for controller to verify deserialization, validation annotations, and response serialization
- Add test for null command passed to `SubmitCorporateBulkOrderService.execute()`

---

## 8. Documentation Consistency: PASS WITH WARNING

**Correct:** File listing, mapping boundary diagram, intentionally-deferred features (7 items), 4 existing open questions.

**Missing OPEN QUESTION** — add to the Open Questions section:

> OPEN QUESTION: `submittedByUserId` is currently accepted from the request body as a temporary measure. When Keycloak/Spring Security authentication is added, this field must be removed from the request payload and derived from the JWT subject claim. A `CustomerAccessPort` should validate that the authenticated user has the right to submit orders for `customerId`. Missing/invalid tokens → 401; valid token but no customer access → 403.

---

## Recommendations

1. **Add OPEN QUESTION about `submittedByUserId` security migration** to the implementation note. This is the only material finding.
2. **Add `@WebMvcTest`** for `SubmitCorporateBulkOrderController` (low priority).
3. **Add null-command test** in `SubmitCorporateBulkOrderServiceTest` (low priority).

---

## Decision

**ARB-007 may be marked [DONE]** after adding one OPEN QUESTION entry about `submittedByUserId` security migration to the implementation note. No code changes required. No blocker exists.
