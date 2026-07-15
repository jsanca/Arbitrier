# Data Knowledge

## Purpose

Navigate durable persistent concepts, data ownership, and datasets without
turning this section into a copy of database migrations or entity mappings.

## Scope

Future pages may describe data ownership, persistent concepts, datasets, and
table relationships when those concepts are stable and useful outside a single
implementation change.

## Authoritative Sources

- [Schema-per-service ADR](../../docs/adr/ADR-0003-schema-per-service-postgres.md)
- [Order-service initial schema migration](../../server/order-service/src/main/resources/db/migration/order_service/V1__create_order_tables.sql)
- Service migrations under `server/*-service/src/main/resources/db/migration/`

## Navigation

No dataset or table pages are curated yet. Add them only when they clarify a
durable business or integration concept; migrations and code remain authoritative.
