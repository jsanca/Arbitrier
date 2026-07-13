Task: ARB-021-FIX-001 — Outbox Architecture Cleanup

Status:
[PLANNED]

Owner:
Mini

Goal:
Apply the small architectural cleanups identified after the ARB-021 review.

Scope:
Only scanning configuration and serializer wiring.
Do not redesign Outbox/Inbox.
Do not implement Kafka.

1. Replace broad scanning

Replace:

@EntityScan(basePackages = "com.arbitrier")
@EnableJpaRepositories(basePackages = "com.arbitrier")

with explicit marker-class scanning per service.

Each service should scan only:
- its own JPA entities/repositories
- platform Outbox/Inbox JPA entities/repositories

Prefer basePackageClasses over package strings.

2. Canonical serializer abstraction

Keep all application and platform consumers coupled only to EventSerializer.

Do not inject ObjectMapper outside the Jackson implementation.

Ensure JacksonEventSerializer receives its ObjectMapper through construction/configuration rather than creating it internally.

Create one canonical messaging ObjectMapper bean/configuration for EventSerializer.

The rest of the system must not depend directly on ObjectMapper.

3. Preserve isolation

The messaging serializer may have its own configuration, but it must remain encapsulated behind EventSerializer.

Do not expose JsonNode, ObjectMapper, JsonProcessingException, or Jackson-specific types through platform contracts.

4. Tests

Add/update tests proving:
- explicit scanning still starts each service context
- Outbox/Inbox repositories are discovered
- unrelated service entities are not scanned
- EventSerializer still round-trips supported events
- no non-infrastructure package depends directly on ObjectMapper

5. Documentation

Update:
- docs/implementation/ARB-021-outbox-inbox-foundation.md
- completion report if needed

Document:
- explicit marker-class scanning
- EventSerializer as the stable system abstraction
- Jackson as replaceable infrastructure

Out of scope:
- No command outbox decision
- No Kafka
- No Avro
- No drainer
- No polling
- No correlation/causation wiring
- No Flyway redesign
- No platform module split

Acceptance:
- Build passes
- Existing tests remain green
- New tests cover scanning and serializer boundaries
- No broad com.arbitrier scanning remains
- ObjectMapper is confined to Jackson infrastructure
- Timebox: stop at 45 minutes and report progress if incomplete