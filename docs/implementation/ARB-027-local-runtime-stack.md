# ARB-027 — Local Runtime Stack

## Outcome

ARB-027 provides a reproducible, local-only Docker Compose runtime for PostgreSQL, Kafka, Confluent Schema Registry, Keycloak, and Kafka UI. It contains infrastructure configuration and synthetic identity data only; no Java application logic, persistence adapters, business tables, business seed data, or canonical Kafka topics were added.

## Runtime decisions

- Kafka runs as one combined broker/controller using KRaft. The container-only `CONTROLLER` listener participates in quorum, `INTERNAL` is advertised as `kafka:29092`, and host clients use `localhost:9092` through `EXTERNAL`.
- Images are pinned: PostgreSQL 18.2, Confluent Platform 8.3.0 (Kafka 4.3 and Schema Registry), Keycloak 26.7.0, and Kafka UI 0.7.2.
- Every exposed port binds only to loopback. All containers share the dedicated `arbitrier-local` network.
- Named volumes persist PostgreSQL and Kafka data. Keycloak persists in its separate PostgreSQL database rather than a second database container.
- Health-based dependencies gate Schema Registry, Keycloak, and Kafka UI. PostgreSQL, Kafka, Schema Registry, and Keycloak have explicit health checks.
- Schema Registry defaults to `BACKWARD` compatibility. Future producers should retain Confluent `TopicNameStrategy`; schema registration belongs to application Kafka adapters.
- Topic bootstrapping is deferred because ADR-0002 and ADR-0004 leave the complete canonical topic set open. This avoids institutionalizing task examples as contracts.
- Lightweight observability services were not added because none had been agreed for this task.

## Data and identity boundary

The `arbitrier` database has five empty, separately owned schemas: `order_service`, `inventory_service`, `credit_service`, `orchestrator_service`, and `platform`. Keycloak has a distinct `keycloak` database and owner. Public schema creation is revoked, and no cross-schema grants or foreign keys are created.

The imported `arbitrier` realm supplies the three requested clients, four realm roles, and four synthetic local users. Import is startup-idempotent: Keycloak skips an existing realm. A volume reset is the documented way to apply changes to the realm file.

## Files

- `infra/docker/docker-compose.yml` — pinned services, network, volumes, health and dependency conditions
- `infra/docker/init-db.sql` — roles, databases, empty schemas, ownership and grants
- `infra/docker/keycloak/arbitrier-realm.json` — local realm, clients, roles and users
- `infra/docker/{compose,start,stop,reset,health,logs}.sh` — portable operations
- `.env.example` — Compose defaults and host application integration contract
- `infra/docker/README.md` — operator guide, credentials, URLs and troubleshooting

## Verification

Static verification uses `docker compose config`, JSON parsing, shell syntax checks, and inspection for unpinned images. Runtime verification is performed with:

```bash
infra/docker/start.sh
infra/docker/health.sh
infra/docker/compose.sh exec -T postgres psql -U arbitrier_admin -d arbitrier -c '\dn+'
curl -fsS http://localhost:8081/config
curl -fsS http://localhost:8180/realms/arbitrier/.well-known/openid-configuration
```

Reset with `infra/docker/reset.sh`. ARB-019 owns persistence adapters, ARB-020 owns database migrations and synthetic business data, and ARB-022 owns Kafka adapters and eventual topic provisioning. ARB-028 was not started.
