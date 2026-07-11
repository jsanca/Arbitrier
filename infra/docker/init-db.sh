#!/usr/bin/env sh
set -eu

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set=ARBITRIER_SERVICE_PASSWORD="$ARBITRIER_SERVICE_PASSWORD" \
  --set=KEYCLOAK_DB_PASSWORD="$KEYCLOAK_DB_PASSWORD" \
  --file=/docker-entrypoint-initdb.d/init-db.sql
