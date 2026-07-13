#!/usr/bin/env bash
# Reset the local Arbitrier stack: wipe volumes, restart services.
# Flyway migrations run automatically on service startup.
# Seed data can be loaded optionally after services are healthy.
#
# Usage:
#   ./infra/docker/reset.sh              # reset only
#   ./infra/docker/reset.sh --seed       # reset + load seed data

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE="$SCRIPT_DIR/compose.sh"

echo "==> Stopping and removing volumes..."
"$COMPOSE" down -v --remove-orphans

echo "==> Starting services..."
"$COMPOSE" up -d --wait

echo "==> Waiting for PostgreSQL to be healthy..."
until "$COMPOSE" exec -T postgres pg_isready -U arbitrier_admin -d arbitrier >/dev/null 2>&1; do
  sleep 2
done

echo "==> PostgreSQL is ready."
echo ""
echo "    Flyway migrations will run automatically when each service starts."
echo "    Start services with: mvn spring-boot:run (or your IDE)"
echo ""

if [[ "${1:-}" == "--seed" ]]; then
  echo "==> Loading seed data..."
  "$COMPOSE" exec -T postgres \
    psql -U arbitrier_admin -d arbitrier \
    -f /dev/stdin < "$SCRIPT_DIR/seed/seed.sql"
  echo "==> Seed data loaded."
fi

echo "==> Reset complete."
