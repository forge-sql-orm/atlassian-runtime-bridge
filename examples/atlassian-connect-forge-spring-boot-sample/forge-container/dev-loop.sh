#!/bin/bash
set -euo pipefail

cleanup() {
  echo "Stopping containers..."
  docker compose down
}
trap cleanup SIGINT SIGTERM EXIT

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi
: "${APP_ID:?Set APP_ID in .env (see .env.example)}"
: "${ENV_ID:?Set ENV_ID in .env (see .env.example)}"
export TAG="${TAG:-latest}"
: "${ENV_NAME:?Set ENV_NAME to your Forge environment (e.g. development)}"

forge containers docker-login
docker pull forge-ecr.services.atlassian.com/forge-platform/proxy-sidecar:latest

docker compose up -d

forge tunnel -e "$ENV_NAME"
