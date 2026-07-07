# contracts

Shared Avro schemas and OpenAPI specifications for all inter-service communication.

## Contents

```
contracts/
├── avro/
│   ├── order/
│   │   └── order-placed-v1.avsc              # Placeholder
│   ├── credit/
│   │   ├── credit-reservation-requested-v1.avsc  # Placeholder
│   │   ├── credit-reserved-v1.avsc               # Placeholder
│   │   └── credit-reservation-denied-v1.avsc     # Placeholder
│   └── inventory/
│       ├── inventory-reservation-requested-v1.avsc # Placeholder
│       ├── inventory-reserved-v1.avsc              # Placeholder
│       └── inventory-reservation-failed-v1.avsc    # Placeholder
└── openapi/
    ├── order-service-api-v1.yaml             # Placeholder
    └── inventory-service-api-v1.yaml         # Placeholder
```

## Schema Versioning Policy

- Schema files are named `<subject>-v<N>.avsc`.
- Breaking changes require a new version (`-v2.avsc`); the old version stays until all consumers migrate.
- Schema compatibility mode: **BACKWARD** (consumers can read data produced with the previous schema version).
- Generated Java Avro classes are produced at build time via the Avro Maven plugin; generated sources are **not committed**.

## Kafka Topic Naming Convention

```
<domain>.<event-name>.<version>
```

Examples:
- `order.placed.v1`
- `credit.reservation.requested.v1`
- `inventory.reserved.v1`

## Status

`ARB-001` — Schema placeholders. No `.avsc` files yet; will be added in service implementation tasks.
