# Arbitrier local runtime

This Compose stack supplies infrastructure for local development. Applications run separately from Maven or an IDE. It intentionally creates no business tables, topics, or application adapters.

## Start and operate

Prerequisites are Docker Desktop or Docker Engine with Compose v2. Copy `.env.example` to `.env` only when overriding defaults.

```bash
infra/docker/start.sh
infra/docker/health.sh
infra/docker/logs.sh                 # or append a service name
infra/docker/stop.sh
infra/docker/reset.sh                # deletes local volumes, imports a fresh realm
```

The scripts use root `.env` when present and otherwise use the safe development defaults in `.env.example`. All host ports bind to `127.0.0.1`.

| Service | Image | Host endpoint |
|---|---|---|
| PostgreSQL | `postgres:18.2` | `localhost:5432` |
| Kafka (KRaft) | `confluentinc/cp-kafka:8.3.0` | `localhost:9092` |
| Schema Registry | `confluentinc/cp-schema-registry:8.3.0` | <http://localhost:8081> |
| Keycloak | `quay.io/keycloak/keycloak:26.7.0` | <http://localhost:8180> |
| Kafka UI | `provectuslabs/kafka-ui:v0.7.2` | <http://localhost:8088> |

Kafka uses a single combined broker/controller in KRaft mode. Host clients use `localhost:9092`; Compose services use `kafka:29092`. The controller listener is internal only. ZooKeeper is neither required nor started.

## PostgreSQL

Database `arbitrier` contains empty schemas `order_service`, `inventory_service`, `credit_service`, `orchestrator_service`, and `platform`. Each schema is owned by a same-named login role. All service roles use development password `local-service-only`. Keycloak uses its own `keycloak` database and role with password `local-keycloak-only`. The admin login is `arbitrier_admin` / `local-admin-only`.

No cross-schema foreign keys, extensions, business tables, or business seed data are created. ARB-019 owns persistence adapters, and ARB-020 owns migrations and synthetic business data.

For a host-run service, select its schema and username while retaining the shared database, for example `DB_SCHEMA=inventory_service` and `DB_USERNAME=inventory_service`.

## Keycloak development identities

Realm: `arbitrier`. Admin console credentials: `admin` / `local-admin-only`.

| User | Password | Realm roles |
|---|---|---|
| `acme-buyer` | `buyer-local` | `CUSTOMER_USER` |
| `acme-admin` | `customer-admin-local` | `CUSTOMER_USER`, `CUSTOMER_ADMIN` |
| `operations-user` | `operations-local` | `OPERATIONS_USER` |
| `operations-admin` | `operations-admin-local` | `OPERATIONS_USER`, `OPERATIONS_ADMIN` |

Clients are `arbitrier-customer-portal`, `arbitrier-admin-console`, and bearer-only `arbitrier-api`. These passwords and identities are synthetic and development-only.

Startup import is idempotent by skipping import when the realm already exists. Changes to the JSON therefore require `infra/docker/reset.sh`; reset deletes the PostgreSQL volume and all local state.

## Schema Registry and topics

The registry default is `BACKWARD` compatibility. Future Avro producers should use `TopicNameStrategy`, producing `<topic>-value` subjects (and `<topic>-key` when an Avro key is introduced). Inspect it with:

```bash
curl -fsS http://localhost:8081/config
curl -fsS http://localhost:8081/subjects
curl -fsS http://localhost:8081/subjects/NAME/versions/latest
```

Kafka UI exposes topics, messages, consumer groups, and the registry. ARB-027 does not bootstrap topics because ADR-0002 and ADR-0004 still leave the canonical full topic set open; adapters/application provisioning will own topic creation under ARB-022. Kafka auto-creation remains enabled for local experimentation.

## Application environment contract

`.env.example` defines `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_SCHEMA`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `SCHEMA_REGISTRY_URL`, `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_JWK_SET_URI`, `KEYCLOAK_REALM`, and `KEYCLOAK_CLIENT_ID`. Host-run services use the values as written. A containerized application would use Compose names (`postgres`, `kafka`, `schema-registry`, `keycloak`) and container ports instead.

## Troubleshooting

- Port collision: change the corresponding port in root `.env` and restart.
- Realm edits not visible: run `infra/docker/reset.sh`; startup import never overwrites an existing realm.
- Startup timeout: inspect `infra/docker/logs.sh SERVICE`, then retry `infra/docker/health.sh`.
- Stale data after SQL changes: initialization scripts run only on a fresh volume, so reset the stack.
- ARM laptop image issue: ensure Docker Desktop is current; the pinned images publish multi-architecture variants.

This is local-only infrastructure: no TLS, authentication on Kafka/Kafka UI, production secret management, observability stack, or application containers are included.
