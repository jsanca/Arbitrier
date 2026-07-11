#!/usr/bin/env sh
set -eu
"$(dirname "$0")/compose.sh" logs -f "$@"
