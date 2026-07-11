#!/usr/bin/env sh
set -eu

COMPOSE="$(dirname "$0")/compose.sh"

"$COMPOSE" ps
"$COMPOSE" exec -T postgres sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
"$COMPOSE" exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:29092 >/dev/null
"$COMPOSE" exec -T schema-registry cub sr-ready localhost 8081 10
"$COMPOSE" exec -T keycloak bash -c 'exec 3<>/dev/tcp/127.0.0.1/9000; printf "GET /health/ready HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n" >&3; grep -q "status.*UP" <&3'

echo "All required Arbitrier local services are healthy."
