#!/usr/bin/env bash
set -euo pipefail

SERVER_PID=""

cleanup() {
    echo ""
    echo "Shutting down server..."
    if [ -n "$SERVER_PID" ] && kill -0 "$SERVER_PID" 2>/dev/null; then
        kill "$SERVER_PID"
        wait "$SERVER_PID" 2>/dev/null || true
    fi
    echo "Done."
}
trap cleanup EXIT INT TERM

echo "Starting server..."
./gradlew :server:bootRun &
SERVER_PID=$!

echo "Waiting for server to be ready..."
elapsed=0
until curl -sf http://localhost:8080/api/v1/sessions > /dev/null 2>&1; do
    if [ $elapsed -ge 60 ]; then
        echo "Server did not start within 60 seconds"
        exit 1
    fi
    sleep 2
    elapsed=$((elapsed + 2))
done
echo "Server is ready on http://localhost:8080"

echo "Launching Android app..."
make android-run

echo "Dev environment running. Press Ctrl+C to stop."
wait "$SERVER_PID"
