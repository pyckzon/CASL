#!/usr/bin/env bash
# Smoke test de Fase 10 — arranque completo de CASL en kind.
set -euo pipefail

PF_PIDS=()
cleanup() {
  for pid in "${PF_PIDS[@]:-}"; do kill "$pid" 2>/dev/null || true; done
  kind delete cluster --name casl-prod 2>/dev/null || true
}
trap cleanup EXIT

# --- Preflight ---
for c in kind helm kubectl promtool amtool trivy jq curl openssl; do
  command -v "$c" >/dev/null 2>&1 || { echo "FALTA: $c"; exit 1; }
done

# --- Validación estática ---
helm dependency update infra/charts/casl
helm lint infra/charts/casl
helm template casl infra/charts/casl > /tmp/casl-rendered.yaml
promtool check rules infra/observability/alert-rules.yml
amtool check-config infra/observability/alertmanager.yml

# --- Cluster ---
kind create cluster --config infra/kind/kind-config.yaml --wait 180s

# --- Prerrequisitos (secretos/configmaps creados fuera de este script) ---
# Ver docs/production/README.md secciones 2-3b.
kubectl create namespace casl 2>/dev/null || true
for s in casl-postgres casl-keycloak casl-order-client; do
  kubectl -n casl get secret "$s" >/dev/null 2>&1 || { echo "FALTA secret: $s (ver README sec.2)"; exit 1; }
done
for cm in casl-keycloak-realm casl-datasources casl-dashboards; do
  kubectl -n casl get configmap "$cm" >/dev/null 2>&1 || { echo "FALTA configmap: $cm (ver README sec.3b)"; exit 1; }
done

# --- Despliegue ---
helm upgrade --install casl infra/charts/casl -n casl --create-namespace --wait --timeout 10m
kubectl -n casl wait --for=condition=Available deployment --all --timeout=5m
helm test casl --logs

# --- Port-forwards (background) ---
pf() { kubectl -n casl port-forward "$@" >/dev/null 2>&1 & PF_PIDS+=($!); }
pf svc/casl-gateway 8080:8080
pf svc/casl-prometheus 9090:9090
pf svc/casl-grafana 3000:3000
pf svc/casl-loki 3100:3100
pf svc/casl-tempo 3200:3200
pf svc/casl-alertmanager 9093:9093
# Mailpit vive en el perfil observability de Compose (host:8025); no hay Service en el cluster.

# --- Espera de readiness ---
for url in http://localhost:9090/-/ready http://localhost:3000/api/health http://localhost:3100/ready http://localhost:3200/ready http://localhost:9093/-/ready; do
  ok=0
  for i in $(seq 1 60); do
    if curl -fsS "$url" >/dev/null 2>&1; then ok=1; break; fi
    sleep 2
  done
  [ "$ok" = "1" ] || { echo "NO READY: $url"; exit 1; }
done

# --- Health de los 8 servicios (management 9090) ---
for s in casl-users-service casl-catalog-service casl-cart-service casl-order-service casl-payment-service casl-notification-service casl-shipping-service casl-gateway; do
  status=$(kubectl -n casl exec "deploy/$s" -- wget -qO- http://localhost:9090/actuator/health 2>/dev/null | jq -r .status || echo "UNKNOWN")
  echo "$s -> $status"
  [ "$status" = "UP" ] || exit 1
done

# --- Métricas: 8 up ---
count=$(curl -sG http://localhost:9090/api/v1/query --data-urlencode 'query=sum(up{job=~"casl-.*"})' | jq -r '.data.result[0].value[1]')
echo "casl up count=$count"
[ "$count" = "8" ] || exit 1

# --- Logs correlacionados (Loki) ---
curl -sG http://localhost:3100/loki/api/v1/query_range --data-urlencode 'query={service="casl-order-service"} |= "traceId"' | jq -e '.data.result | length > 0' >/dev/null

# --- Trazas (Tempo) ---
trace_id=$(curl -s 'http://localhost:3200/api/search?q=%7Bresource.service.name%3D%22casl-gateway%22%7D' | jq -r '.traces[0].traceID')
test -n "$trace_id" && test "$trace_id" != "null"
curl -s "http://localhost:3200/api/traces/$trace_id" | jq -e '.batches | length > 0' >/dev/null

# --- Dashboard ---
GRAFANA_PASS="${GRAFANA_ADMIN_PASSWORD:?GRAFANA_ADMIN_PASSWORD must be set}"
curl -s -u "admin:${GRAFANA_PASS}" http://localhost:3000/api/dashboards/uid/casl-overview | jq -e '.dashboard.uid == "casl-overview"' >/dev/null

# --- Alerta de prueba en Mailpit ---
curl -sX POST http://localhost:9093/api/v2/alerts -H 'Content-Type: application/json' -d '[{"labels":{"alertname":"CaslHighHttpErrorRate","service":"casl-test","severity":"high"},"annotations":{"summary":"smoke test"}}]'
sleep 40
mail_count=$(curl -s http://localhost:8025/api/v1/messages | jq '[.messages[] | select(((.Subject // "") + " " + (.Snippet // "")) | contains("smoke test"))] | length')
echo "mailpit messages=$mail_count"
[ "$mail_count" -ge 1 ] || exit 1

echo "SMOKE OK"