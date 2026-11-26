# Fase 10 — Producción CASL

Pasos de arranque y verificación en un cluster local (`kind`).

## Prerrequisitos
- Docker Desktop 4.x (engine 29.6.x)
- `kind`, `helm`, `kubectl`, `promtool`, `amtool`, `trivy`, `jq`, `curl`, `openssl`
- Imágenes CASL construidas: `.\gradlew.bat :services:<servicio>:bootBuildImage --no-daemon` para los 8

## 1. Cluster
```bash
kind create cluster --config infra/kind/kind-config.yaml --wait 180s
```

## 2. Secretos (Vault dev + ESO)
Seguir `infra/k8s/external-secrets/README.md`. Resumen:
```bash
helm repo add external-secrets https://charts.external-secrets.io
helm upgrade --install external-secrets external-secrets/external-secrets -n external-secrets --create-namespace --version 2.15.0 --wait
helm repo add hashicorp https://helm.releases.hashicorp.com
helm upgrade --install vault hashicorp/vault -n vault --create-namespace --version 0.34.1 --set server.dev.enabled=true --wait
export VAULT_TOKEN="$(openssl rand -hex 32)"
kubectl -n casl create secret generic vault-token --from-literal=token="$VAULT_TOKEN"
# escribir secretos en Vault (ver README)
kubectl apply -f infra/k8s/external-secrets/
```

## 2b. Imagen Keycloak optimizada
```bash
docker build -t casl/keycloak:26.8 infra/k8s/keycloak/
kind load docker-image casl/keycloak:26.8 --name casl-prod
```

## 2c. PostgreSQL
```bash
kubectl apply -f infra/k8s/postgres.yaml
```

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
   El realm se importa con placeholders `${KC_CASL_ORDER_SECRET}` resueltos por ESO desde Vault.

## 3b. Observabilidad (ConfigMaps + manifests)
```bash
kubectl apply -f infra/k8s/observability/
```
(Alternativa equivalente: `kubectl -n casl create configmap casl-alert-rules --from-file=infra/observability/alert-rules.yml`, etc.)

## 4. Despliegue CASL
```bash
helm upgrade --install casl infra/charts/casl -n casl --create-namespace --wait --timeout 10m
```

## 5. Verificación
```bash
bash scripts/smoke.sh
```

## 6. Escaneo de IaC
```bash
helm template casl infra/charts/casl > /tmp/rendered
trivy config --severity CRITICAL,HIGH --ignore-unfixed --exit-code 1 --ignorefile .trivyignore.yaml /tmp/rendered
```

## Referencias
- ADR-021: `docs/architecture/decisions/021-produccion-kind-helm-eso.md`
- Investigación: `Researcher` (2026-11-25)