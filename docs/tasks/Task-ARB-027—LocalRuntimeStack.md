Task: ARB-027 — Local Runtime Stack

Status:
[COMPLETE]

Owner:
Elito

Context:

Arbitrier currently contains business/application logic and in-memory test adapters.

ARB-027 creates a reproducible local development runtime using Docker Compose.

This task provides infrastructure only.

It must not implement business logic or production application adapters.

Goal:

Create a stable local runtime stack containing:

- PostgreSQL
- Kafka
- Schema Registry
- Keycloak
- Kafka UI
- optional observability services if already agreed and lightweight

Primary scope:

- infra/docker/
- environment templates
- local runtime documentation

Coordinate schema names, ports, credentials, and health checks with ARB-019.

--------------------------------------------------

1. Docker Compose services

Provide:

- PostgreSQL
- Kafka
- Schema Registry
- Keycloak
- Kafka UI

Use current supported versions compatible with:

- Spring Boot 4.1
- Java 25
- Spring Kafka
- Avro
- Keycloak integration

Pin image versions.

Do not use floating latest tags.

--------------------------------------------------

2. Kafka mode

Prefer modern Kafka KRaft mode unless there is a documented compatibility reason to keep
ZooKeeper.

Document the decision.

If using KRaft:

- configure controller/broker roles correctly;
- expose local listener;
- expose container-network listener;
- ensure Schema Registry and Kafka UI use internal listener.

--------------------------------------------------

3. PostgreSQL

Create a single PostgreSQL instance for local simplicity.

Create schemas:

- order_service
- inventory_service
- credit_service
- orchestrator_service
- platform if later required
- keycloak database/schema separately

Prefer separate databases or users only where useful.

At minimum:

- separate service schemas;
- explicit ownership;
- no cross-schema business foreign keys.

Provide initialization scripts only for:

- schemas;
- users/permissions;
- extensions if required.

Do not create production business tables if ARB-019/020 owns migrations.

--------------------------------------------------

4. Keycloak

Configure local Keycloak with an importable realm.

Create:

Realm:
- arbitrier

Clients:
- arbitrier-customer-portal
- arbitrier-admin-console
- arbitrier-api

Roles:
- CUSTOMER_USER
- CUSTOMER_ADMIN
- OPERATIONS_USER
- OPERATIONS_ADMIN

Create sample local users only for development.

Example:

- customer buyer for ACME
- customer admin for ACME
- operations user
- operations admin

Do not use real passwords or secrets.

Document all local credentials clearly as development-only.

Ensure realm import is idempotent or document reset behavior.

--------------------------------------------------

5. Schema Registry

Configure Schema Registry to connect to Kafka.

Document:

- local URL;
- compatibility default;
- expected subject naming strategy;
- how to inspect registered schemas.

Do not register schemas manually unless there is a small bootstrap script clearly owned by
this task.

Prefer application registration during future Kafka adapter execution.

--------------------------------------------------

6. Kafka UI

Expose Kafka UI locally.

Configure it to display:

- cluster;
- topics;
- consumer groups;
- messages;
- Schema Registry integration if supported.

No authentication required for local-only development unless trivial.

--------------------------------------------------

7. Networking

Use one dedicated Docker network.

Service containers should reference each other by Compose service name.

Expose only necessary host ports.

Suggested local defaults may include:

- PostgreSQL: 5432
- Kafka external: 9092
- Schema Registry: 8081
- Keycloak: 8080 or another non-conflicting port
- Kafka UI: 8088

Resolve conflicts with application service ports.

Document the final port map.

--------------------------------------------------

8. Volumes

Create named volumes for:

- PostgreSQL
- Kafka
- Keycloak database if separate

Provide a documented reset command.

Example:

docker compose down -v

Do not commit runtime data.

--------------------------------------------------

9. Health checks

Add health checks for:

- PostgreSQL
- Kafka
- Schema Registry
- Keycloak

Use depends_on health conditions where supported.

Avoid relying only on container-started status.

Provide a small verification script if useful.

--------------------------------------------------

10. Environment variables

Create:

- .env.example

Include:

- local ports;
- PostgreSQL credentials;
- Kafka bootstrap servers;
- Schema Registry URL;
- Keycloak realm/client configuration.

Do not commit real secrets.

Use obvious development-only defaults.

--------------------------------------------------

11. Application integration contract

Document environment variables expected by services:

PostgreSQL:
- DB_HOST
- DB_PORT
- DB_NAME
- DB_SCHEMA
- DB_USERNAME
- DB_PASSWORD

Kafka:
- KAFKA_BOOTSTRAP_SERVERS
- SCHEMA_REGISTRY_URL

Keycloak:
- KEYCLOAK_ISSUER_URI
- KEYCLOAK_JWK_SET_URI if needed
- KEYCLOAK_REALM
- KEYCLOAK_CLIENT_ID

Align names with existing Spring configuration where possible.

Do not modify Java application logic unless only documentation/config placeholders are needed.

--------------------------------------------------

12. Synthetic infrastructure data

Allowed:

- Keycloak local users/roles;
- empty schemas;
- Kafka topic bootstrap if explicitly useful.

Do not create business seed data for:

- orders;
- inventory;
- credit;
- sagas.

That belongs to ARB-020 Database Migrations & Synthetic Data.

--------------------------------------------------

13. Topic bootstrap

If topics are created locally, use the canonical topic naming conventions already documented.

At minimum consider:

- order.created.v1
- stock.reserved.v1
- stock.partially-reserved.v1
- stock.rejected.v1
- credit.approved.v1
- credit.rejected.v1
- release-stock commands
- release-credit commands
- order-confirm commands

Do not invent names without checking ADRs/contracts.

It is acceptable to defer automatic topic creation and allow application bootstrap later.

Document the choice.

--------------------------------------------------

14. Scripts

Provide simple scripts or Make targets for:

- start stack;
- stop stack;
- reset stack;
- inspect health;
- tail logs.

Examples:

- infra/docker/start.sh
- infra/docker/stop.sh
- infra/docker/reset.sh
- infra/docker/health.sh

Keep scripts portable where possible.

--------------------------------------------------

15. Documentation

Create:

- docs/implementation/ARB-027-local-runtime-stack.md

Update:

- infra/docker/README.md
- root README local-development section
- .env.example
- docs/okf/index.md if applicable

Document:

- prerequisites;
- start command;
- port map;
- local credentials;
- service URLs;
- reset procedure;
- health verification;
- troubleshooting;
- ownership boundaries with ARB-019/020/022.

--------------------------------------------------

Out of scope

- No Kubernetes.
- No Terraform.
- No GCP.
- No Helm.
- No production secrets.
- No production TLS.
- No business database migrations.
- No JPA entities.
- No Kafka Java adapters.
- No application deployment containers.
- No CI/CD.
- No Prometheus/Grafana/Tempo unless already present and explicitly lightweight.
- No ARB-028 work.

--------------------------------------------------

Acceptance Criteria

- docker compose up starts the complete local stack.
- PostgreSQL is healthy.
- Kafka is healthy.
- Schema Registry is healthy.
- Keycloak realm imports successfully.
- Kafka UI can inspect the cluster.
- service schemas exist with correct ownership.
- no business tables are created prematurely.
- local credentials are documented.
- environment template exists.
- reset is reproducible.
- no secrets are committed.
- documentation is complete.
- ready for local application-adapter integration.

After completion:

Report:

- image versions;
- services;
- port map;
- schemas/users;
- Keycloak realm/clients/roles/users;
- scripts;
- verification performed;
- known issues.

Do not start ARB-028.
