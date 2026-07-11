#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE="$ROOT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
  ENV_FILE="$ROOT_DIR/.env.example"
fi

exec docker compose --env-file "$ENV_FILE" -f "$SCRIPT_DIR/docker-compose.yml" "$@"
