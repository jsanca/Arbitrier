#!/usr/bin/env sh
set -eu
"$(dirname "$0")/compose.sh" up -d --wait
