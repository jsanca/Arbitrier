-- Local infrastructure only. ARB-019/020 own business tables and seed data.
-- psql variables are supplied by the official PostgreSQL entrypoint environment.

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'order_service') THEN
    CREATE ROLE order_service LOGIN;
  END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'inventory_service') THEN
    CREATE ROLE inventory_service LOGIN;
  END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'credit_service') THEN
    CREATE ROLE credit_service LOGIN;
  END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'orchestrator_service') THEN
    CREATE ROLE orchestrator_service LOGIN;
  END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'platform') THEN
    CREATE ROLE platform LOGIN;
  END IF;
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'keycloak') THEN
    CREATE ROLE keycloak LOGIN;
  END IF;
END
$$;

SELECT format('ALTER ROLE %I PASSWORD %L', role_name, :'ARBITRIER_SERVICE_PASSWORD')
FROM (VALUES ('order_service'), ('inventory_service'), ('credit_service'),
             ('orchestrator_service'), ('platform')) AS roles(role_name) \gexec
SELECT format('ALTER ROLE keycloak PASSWORD %L', :'KEYCLOAK_DB_PASSWORD') \gexec

CREATE SCHEMA IF NOT EXISTS order_service AUTHORIZATION order_service;
CREATE SCHEMA IF NOT EXISTS inventory_service AUTHORIZATION inventory_service;
CREATE SCHEMA IF NOT EXISTS credit_service AUTHORIZATION credit_service;
CREATE SCHEMA IF NOT EXISTS orchestrator_service AUTHORIZATION orchestrator_service;
CREATE SCHEMA IF NOT EXISTS platform AUTHORIZATION platform;

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT CONNECT ON DATABASE arbitrier TO order_service, inventory_service, credit_service,
  orchestrator_service, platform;

SELECT 'CREATE DATABASE keycloak OWNER keycloak'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'keycloak') \gexec
REVOKE ALL ON DATABASE keycloak FROM PUBLIC;
GRANT CONNECT ON DATABASE keycloak TO keycloak;
