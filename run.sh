#!/usr/bin/env bash
# Run Whisper locally with .env loaded.
# Usage: ./run.sh

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load .env if present
if [ -f "$SCRIPT_DIR/.env" ]; then
    set -a
    source "$SCRIPT_DIR/.env"
    set +a
    echo "Loaded .env"
else
    echo "No .env found (running without OAuth, all endpoints open)"
fi

exec java \
    -Djava.library.path="$SCRIPT_DIR/wordgen/target/release" \
    -jar "$SCRIPT_DIR/service/target/whisper-service-0.0.1-SNAPSHOT.jar"
