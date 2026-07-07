-- Database initialization for local development.
-- Creates per-service schemas and a dedicated Keycloak schema.
-- Run automatically by the postgres container on first start.

CREATE SCHEMA IF NOT EXISTS order_service;
CREATE SCHEMA IF NOT EXISTS inventory_service;
CREATE SCHEMA IF NOT EXISTS credit_service;
CREATE SCHEMA IF NOT EXISTS orchestrator_service;
CREATE SCHEMA IF NOT EXISTS keycloak;
