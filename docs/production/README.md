# Fase 10 — Producción CASL

Pasos de arranque y verificación en un cluster local (`kind`). La fase quedó cerrada en el
PR #43 (`eb5ba92`); esta guía recoge el procedimiento reproducible y los prerrequisitos
que el smoke test comprueba.

## Prerrequisitos
- Docker Desktop 4.x (engine 29.6.x)
- `kind`, `helm`, `kubectl`, `promtool`, `amtool`, `trivy`, `jq`, `curl`, `openssl`
- Imágenes CASL construidas: `.\gradlew.bat :services:<servicio>:bootBuildImage --no-daemon` para los 8

## 1. Cluster
```bash
kind create cluster --config infra/kind/kind-config.yaml --wait 180s
kubectl create namespace casl
```
`scripts/smoke.sh` reutiliza el cluster `casl-prod` si ya existe; en ese caso no hace falta
repetir este paso, pero **sí** dejan creados los secretos/configmap de las secciones 2-3b.

## 2. Secretos (Vault dev + ESO)
Seguir `infra/k8s/external-secrets/README.md`. Resumen:
```bash
helm repo add external-secrets https://charts.external-secrets.io
helm upgrade --install external-secrets external-secrets/external-secrets -n external-secrets --create-namespace --version 2.15.0 --wait
helm repo add hashicorp https://helm.releases.hashicorp.com
export VAULT_TOKEN="$(openssl rand -hex 32)"
helm upgrade --install vault hashicorp/vault -n vault --create-namespace --version 0.34.1 \
  --set server.dev.enabled=true --set server.dev.devRootToken="$VAULT_TOKEN" --wait
kubectl -n casl create secret generic vault-token --from-literal=token="$VAULT_TOKEN"
# escribir secretos en Vault (ver README)
kubectl apply -f infra/k8s/external-secrets/secretstore.yaml \
  -f infra/k8s/external-secrets/external-secret-order.yaml \
  -f infra/k8s/external-secrets/external-secret-postgres.yaml \
  -f infra/k8s/external-secrets/external-secret-keycloak.yaml
```
`server.dev.devRootToken` debe fijarse al valor generado: con el chart 0.34.1 el token por
defecto es `root`, distinto del `$VAULT_TOKEN` aleatorio del shell. `secretstore-prod.yaml`
(HTTPS + Kubernetes auth) no se aplica en el perfil dev.

## 2b. Imagen Keycloak optimizada
```bash
docker build -t casl/keycloak:26.8 infra/k8s/keycloak/
kind load docker-image casl/keycloak:26.8 --name casl-prod
```

## 2c. PostgreSQL
```bash
kubectl apply -f infra/k8s/postgres.yaml
```
El cluster local usa la credencial dev fija `casl/casl` en `infra/k8s/postgres.yaml` y en
su ConfigMap de inicialización; no es una credencial de producción. Antes de llevar este
stack fuera de `kind` hay que sustituirla por el valor materializado por ESO desde Vault.

## 3. Keycloak (HTTPS + PostgreSQL externo)
1. Crear el Secret TLS (certificado autofirmado para `keycloak.casl.local`):
   ```bash
   openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
     -keyout /tmp/tls.key -out /tmp/tls.crt -subj "/CN=keycloak.casl.local"
   kubectl -n casl create secret tls keycloak-tls --cert=/tmp/tls.crt --key=/tmp/tls.key
   ```
2. Crear el ConfigMap con el realm (parametrizado):
   ```bash
   kubectl -n casl create configmap casl-keycloak-realm \
     --from-file=realm-export.json=infra/k8s/keycloak/realm-export.json
   ```
3. Aplicar el resto de manifiestos:
   ```bash
   kubectl apply -f infra/k8s/keycloak/
   ```
   El realm se importa con placeholders `${KC_CASL_*}` que Keycloak expande desde las
   variables de entorno materializadas por ESO desde Vault.

## 3b. Observabilidad (ConfigMaps + manifests)
```bash
kubectl apply -f infra/k8s/observability/
```
Esto crea los ConfigMaps `casl-alert-rules`, `casl-alertmanager`, `casl-datasources` y
`casl-dashboards` (estos dos últimos con las etiquetas de sidecar de Grafana), los
manifiestos de Alloy y Mailpit y la `PrometheusRule` con las cuatro alertas CASL. Las
reglas y la configuración de Alertmanager que despliega el chart están en
`infra/charts/casl/values.yaml`; los ConfigMaps de `infra/k8s/observability/` son la copia
equivalente para el arranque manual. Alternativa: crearlos con
`kubectl -n casl create configmap ... --from-file=...`.

## 4. Despliegue CASL
```bash
helm upgrade --install casl infra/charts/casl -n casl --create-namespace --wait --timeout 10m
```

## 5. Verificación
```bash
export GRAFANA_ADMIN_PASSWORD=...   # mismo valor que el Secret de Grafana
bash scripts/smoke.sh
```
El script valida estática y arranque, comprueba los secretos/configmap anteriores, levantar
los ocho servicios, health, métricas, logs, trazas, dashboard y el correo de prueba en
Mailpit. Reutiliza el cluster `casl-prod` si ya existe y solo lo elimina si lo creó él.

## 6. Escaneo de IaC
```bash
mkdir -p /tmp/rendered
helm dependency update infra/charts/casl
helm template casl infra/charts/casl > /tmp/rendered/rendered.yaml
trivy config --severity CRITICAL,HIGH --exit-code 1 --ignorefile .trivyignore.yaml /tmp/rendered
trivy config --severity CRITICAL,HIGH --exit-code 1 --ignorefile .trivyignore.yaml infra/k8s
```
Las exenciones de subcharts upstream están en `.trivyignore.yaml` y en
`docs/security/exceptions.md` (ADR-021 §8.1); no se usa `--ignore-unfixed` porque el
objetivo es detectar también hallazgos sin fix disponible.

## Referencias
- ADR-021: `docs/architecture/decisions/021-produccion-kind-helm-eso.md`
- Investigación: `Researcher` (2026-11-25)