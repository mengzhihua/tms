#!/usr/bin/env bash
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"
PORT="${SERVER_PORT:-8083}"
echo "Starting TMS 运输管理 on http://127.0.0.1:$PORT"
exec java ${JAVA_OPTS:-} -jar tms-backend-1.0.0.jar --server.port="$PORT" 
