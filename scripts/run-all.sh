#!/usr/bin/env bash
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
docker compose up -d mysql
mvn -pl discovery-service spring-boot:run &
sleep 8
for service in auth-service flood-service drought-service fire-service zoonotic-service mining-service report-service alert-service dashboard-service api-gateway; do
  (mvn -pl "$service" spring-boot:run) &
done
wait
