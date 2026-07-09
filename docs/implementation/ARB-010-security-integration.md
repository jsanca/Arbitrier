# ARB-010 — Security Integration

| Field  | Value       |
|--------|-------------|
| Task   | ARB-010     |
| Status | Implemented |
| Date   | 2026-07-08  |

## Summary

Spring Security Resource Server (JWT/Bearer token) wired into order-service. The authenticated user's identity is derived exclusively from the JWT subject claim — the request body no longer accepts `submittedByUserId`. A `CustomerAccessPort` outbound port validates user-to-customer authorisation before an order is created.

## Files Created

### Platform

| File | Purpose |
|------|---------|
| `platform/security/AuthenticatedUser.java` | Immutable principal record: `userId` (JWT sub) + `authorities`. Framework-agnostic. |
| `platform/security/AuthenticatedUserProvider.java` | Interface for resolving the current user from any security context; implemented in adapter layer. |

### Order Service — main sources

| File | Purpose |
|------|---------|
| `application/OrderProblemCode.java` | `CUSTOMER_ACCESS_DENIED` — maps to HTTP 403 via `ProblemCode.httpStatus()` |
| `application/port/outbound/CustomerAccessPort.java` | Outbound port: `canSubmitOrder(userId, customerId): boolean` |
| `adapter/outbound/customer/AllowAllCustomerAccessAdapter.java` | Placeholder — allows all users; replace when customer-membership is implemented |
| `config/SecurityConfiguration.java` | `@EnableWebSecurity` filter chain: stateless, CSRF-off, JWT resource server, actuator open |

### Order Service — test sources

| File | Purpose |
|------|---------|
| `adapter/inbound/rest/SubmitCorporateBulkOrderControllerTest.java` | 7 controller tests: 401/403/201, identity extraction, spoof-proof, validation |

## Files Modified

| File | Change |
|------|--------|
| `platform/error/ProblemCode.java` | Added `default int httpStatus() { return 422; }` — codes self-declare HTTP status |
| `platform/web/PlatformExceptionHandler.java` | Uses `ex.code().httpStatus()` — no longer hard-codes 422 |
| `order-service/adapter/inbound/rest/CreateOrderRequest.java` | Removed `submittedByUserId` field — JWT subject is the only identity source |
| `order-service/adapter/inbound/rest/SubmitCorporateBulkOrderController.java` | Accepts `Authentication authentication` parameter; extracts `userId = authentication.getName()` |
| `order-service/application/service/SubmitCorporateBulkOrderService.java` | Added `CustomerAccessPort` dependency; throws `CUSTOMER_ACCESS_DENIED` (403) if access denied |
| `order-service/config/OrderServiceConfiguration.java` | Wires `CustomerAccessPort` bean (`AllowAllCustomerAccessAdapter`); passes it to service |
| `order-service/pom.xml` | Added `spring-boot-starter-oauth2-resource-server` + `spring-security-test` (test) |
| `order-service/application.yml` | Added comment documenting `spring.security.oauth2.resourceserver.jwt.issuer-uri` requirement |
| `order-service/test/SubmitCorporateBulkOrderServiceTest.java` | Added 3 `CustomerAccessPort` tests (`DENY_ALL` path); 17 tests total |
| `order-service/test/OrderServiceTestConfiguration.java` | Added mock `JwtDecoder` for test context; removed duplicate `CustomerAccessPort` |

## Architecture

```
HTTP POST /api/orders
  Authorization: Bearer <jwt>
  Body: { "customerId": "...", "lines": [...] }
           ↕  Spring Security validates JWT
  SecurityFilterChain (SecurityConfiguration)
           ↓
  SubmitCorporateBulkOrderController
    authentication.getName() → "user-abc"  ← JWT sub
           ↓
  SubmitCorporateBulkOrderCommand(customerId, "user-abc", lines)
           ↓
  SubmitCorporateBulkOrderService
    customerAccessPort.canSubmitOrder("user-abc", customerId)
      → false  →  ApplicationProblemException(CUSTOMER_ACCESS_DENIED)  →  403
      → true   →  Order.create(...)  →  save  →  publish  →  201
```

## HTTP Status Mapping

| Scenario | Status | Code |
|----------|--------|------|
| Missing / invalid JWT | 401 | Spring Security default |
| Valid JWT, no customer access | 403 | `ORDER_ACCESS_DENIED` |
| Bean validation failure | 400 | `VALIDATION_ERROR` |
| Business rule violation | 422 | typed `ProblemCode` |
| Success | 201 | — |

The `ProblemCode.httpStatus()` default method (added to platform) allows problem codes to self-declare their HTTP status. The exception handler is now generic — no code special-cases are needed in the platform.

## JwtDecoder in Tests

`OrderServiceTestConfiguration` provides a mock `JwtDecoder` that accepts any token string without signature verification:

```java
@Bean
JwtDecoder jwtDecoder() {
    return token -> Jwt.withTokenValue(token)
            .header("alg", "none")
            .claim("sub", "test-user")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
}
```

Controller tests use `spring-security-test`'s `jwt()` post-processor, which injects a `JwtAuthenticationToken` directly — bypassing the filter and the `JwtDecoder`.

## Native Image Considerations

- `@EnableWebSecurity` and `SecurityFilterChain` are processed by Spring AOT at build time — no reflection hints needed.
- Spring Security's JWT filter chain (`NimbusJwtDecoder`) uses JSSE for TLS and JOSE for signature verification. Both libraries require reflection registration when building a native image.
- `JwtAuthenticationConverter` is Spring-managed and AOT-processed; no custom `RuntimeHintsRegistrar` needed for the default configuration.
- OPEN QUESTION: When native image build is attempted (future task), validate that `com.nimbusds:nimbus-jose-jwt` publishes GraalVM native hints or whether a custom registrar is needed (see ADR-0007).

## Resolved Open Question (from ARB-007)

`submittedByUserId` in `CreateOrderRequest` was accepted from the request body as a temporary measure. It is now removed. The JWT subject (`authentication.getName()`) is the sole source of the submitting user's identity — this closes the ARB-007 open question.

## Open Questions

- OPEN QUESTION: Real `CustomerAccessPort` implementation — when customer-membership service is introduced, replace `AllowAllCustomerAccessAdapter` with an adapter that queries the membership store.
- OPEN QUESTION: JWT `sub` claim format — Keycloak uses a UUID by default (`"sub": "f47ac10b-..."`) while `UserId` currently accepts any non-blank string. Validate format alignment when Keycloak is wired.
- OPEN QUESTION: Actuator endpoint security — currently `/actuator/**` is open. Restrict to internal network or add a separate security policy for production.
- OPEN QUESTION: `nimbus-jose-jwt` native image compatibility (see Native Image Considerations above).
