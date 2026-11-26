# ADR-021 — Preparación para producción: kind, Helm, ESO/Vault, Keycloak HTTPS y smoke test

- **Estado:** Aceptado
- **Fecha:** 2026-11-25
- **Decisores:** Architect
- **Contexto:** Fase 10 del roadmap. Fase 9 cerrada (PR #42, `2bc2e5b`).

## Contexto

CASL hoy se despliega únicamente con `docker compose --profile core --profile observability` y
secretos en claro en `compose.yaml`/`realm-export.json`. Fase 10 exige un despliegue reproducible en
un cluster local (`kind`) **sin secretos en Git**, con Keycloak en HTTPS y base de datos externa,
receptores reales de Alertmanager y un smoke test de arranque completo de los ocho servicios y de la
pila de observabilidad (Prometheus, Grafana, Tempo, Loki, Promtail), verificando trazas, logs y
métricas correlacionadas.

Las decisiones de esta ADR provienen de la investigación `Researcher` (2026-11-25) y se apoyan en
las versiones oficiales verificadas (tabla al final).

## Decisión

1. **Cluster local: `kind` v0.33.0** con imagen de nodo `kindest/node:v1.37.0@sha256:a1ed56cf…`
   (pinneada por digest) y `extraPortMappings` 80/443 para Ingress. ≥6 GB RAM (8 GB recomendado).
   Docker Desktop 4.x (engine 29.6.x) es el provider; no hay incompatibilidad documentada.

2. **Packaging: Helm 4.3.1** (fallback 3.22.1) con un chart *umbrella* `casl` que incluye subcharts/
   templates para los ocho desplegables y la observabilidad. `values.yaml` base + `values-<env>.yaml`.
   `helm lint` y `helm test` como gates. (Helm 3.16.x es insuficiente frente a K8s 1.37.)

3. **Secretos: External Secrets Operator v2.15.0 + Vault en modo dev** (chart `external-secrets-2.15.0`
   y `vault-helm` 0.34.1). En Git solo se versionan `SecretStore`/`ExternalSecret` (referencias) y
   los manifiestos de despliegue; los valores se escriben en Vault desde el shell con
   `kubectl create secret`. El token de bootstrap se crea fuera de Git. Sealed Secrets/SOPS
   quedan como alternativas documentadas pero no son el mecanismo principal.

4. **Keycloak en producción:** `start` (sin `--optimized`, para arranque local) con PostgreSQL 17 externo, HTTPS (certificado
   montado desde Secret, self-signed en dev, Let's Encrypt/cert-manager en producción), y
   `--import-realm` con placeholders `${ENV_VAR}` en `realm-export.json`. Se elimina el default
   hardcoded `casl-order-secret` de `KeycloakServiceTokenProvider` y se parametriza
   `KEYCLOAK_ISSUER_URI` (hoy fijado a `http://localhost:8180`) para que use la URL HTTPS pública
   del cluster. Para producción se construye una imagen derivada con `kc.sh build` (https + postgres)
   y se usa `start --optimized`.

5. **Alertmanager:** receptor email a **Mailpit v1.34.0** (SMTP `0.0.0.0:1025`, UI `8025`, REST API)
   en lugar del webhook local `http://127.0.0.1:9099/alerts`. `route`, `group_by`, `inhibit_rules`
   y `send_resolved: true`. `amtool check-config` y `promtool check rules` como gates.

6. **Observabilidad en K8s:** charts oficiales de la comunidad (`kube-prometheus-stack` 93.11.0,
   `grafana-community/loki` 21.6.0, `grafana-community/tempo` 3.2.1, `grafana-community/grafana`
   14.6.0) + **Grafana Alloy** como colector de logs (sustituye Promtail, que usa
   `docker_sd_configs`). Se reutilizan `alert-rules.yml`, `alertmanager.yml`, `datasources.yml` y
   `casl-overview.json` como ConfigMaps/values, conservando los UIDs `casl-*` y la etiqueta `service`
   en las métricas (relabeled desde labels de pod).

7. **Secret `order → payment`:** `client_credentials` se mantiene, pero el secreto real viene del
   Secret materializado por ESO (rotación en Vault con `refreshInterval`).

8. **Escaneo de IaC:** `trivy config` (Trivy 0.76.2) sobre `helm template`, con umbral
   `CRITICAL,HIGH` y la misma allowlist con `expired_at`. Checkov queda como *second opinion*.

9. **Smoke test:** script `scripts/smoke.sh` con `set -euo pipefail`, validación estática,
   creación de cluster, `helm upgrade --install ... --wait`, `helm test`, port-forwards, health checks
   de los 8 servicios, queries a Prometheus/Loki/Tempo, verificación del dashboard y alertas en
   Mailpit.

## Alternativas rechazadas

- **Kustomize o `kubectl apply -f` como mecanismo principal:** sin releases con historial/rollback
  ni `helm lint`/`helm test`; con 8 servicios los overlays se multiplican.
- **minikube / k3d:** mayor huella o k3s-specific behaviour; no aportan ventaja frente a kind con
  Docker Desktop ya presente.
- **Secrets nativos (`kubectl apply`) / SOPS+age / Sealed Secrets:** sin rotación in-cluster o
  con clave ligada al cluster; quedan como fallback documentado.
- **Slack/PagerDuty como receptor en dev:** exige cuenta externa y gestión de token; Mailpit es
  gratuito y 100 % local.
- **Promtail en K8s:** `docker_sd_configs` no válido en K8s; se usa Alloy.
- **Checkov como herramienta primaria:** solapamiento con Trivy ya presente.

## Consecuencias

- (+) Despliegue reproducible en `kind` sin secretos en Git; rotación centralizada en Vault.
- (+) Observabilidad con los UIDs y la etiqueta `service` preservados → dashboard y alertas
  funcionan sin cambios.
- (+) Escaneo de IaC con la misma herramienta y umbral que el resto del pipeline.
- (−) Coste de RAM: kube-prometheus-stack + Loki/Tempo/Alloy consume mucho más que Compose;
  el smoke test puede requerir 8-10 GB para Docker Desktop.
- (−) Alloy añade una curva de aprendizaje frente a Promtail, pero es el camino soportado.
- (−) `start --optimized` exige build previo del Keycloak (imagen con realm importado) en CI.

## Verificación (pendiente de completar)

- `kind create cluster --image ... --wait` + `helm upgrade --install` + `helm test` exit 0.
- `kubectl -n casl wait --for=condition=Available deployment --all` exit 0.
- Health de los 8 servicios en `actuator/health` = UP.
- Prometheus: `up{job=~"casl-.*"}` = 8; Loki/Grafana/Tempo queries devuelven datos.
- Alertmanager: correo en Mailpit para las 4 alertas (o alerta de prueba equivalente).
- `trivy config` sobre `helm template` sin hallazgos CRITICAL/HIGH.
- `git diff --check` + `helm lint` sin errores.
- `unit tests`, `integration tests`, CodeQL en verde (sin cambios de código de negocio).

## Versiones mínimas verificadas (2026-11-25)

| Componente | Recomendado | Mínimo | Fuente |
|---|---|---|---|
| Helm | 4.3.1 (fallback 3.22.1) | ≥3.22.x con K8s 1.37 | helm.sh version_skew |
| kind | v0.33.0 | K8s 1.37.0, Docker Desktop 4.x | kind release notes |
| kubectl | 1.37.x | skew estándar | K8s |
| ESO | v2.15.0 | K8s ≥1.16 | external-secrets.io |
| Vault chart | 0.34.1 / 2.0.4 | `audience` desde Vault 1.21 | vault-helm release |
| Keycloak | 26.8.x | PostgreSQL 14–18 (17 incluido) | keycloak.org/server/db |
| Alertmanager | v0.35.0 | promtool check-config | prometheus.io |
| Mailpit | v1.34.0 | SMTP 1025 / UI 8025 | release notes |
| Trivy | 0.76.2 (proyecto 0.70.0) | `trivy config` | trivy.dev |
| kube-prometheus-stack | 93.11.0 (op. 0.95.2) | `kubeVersion >=1.25` | Chart.yaml |
| grafana-community/loki | 21.6.0 (app 3.7.10) | paridad con Compose | Chart.yaml |
| grafana-community/tempo | 3.2.1 (app 3.1.1) | paridad con Compose | Chart.yaml |
| grafana-community/grafana | 14.6.0 (app 13.3.2) | dashboard schemaVersion 39 | Chart.yaml |
| Promtail → Alloy | Alloy | `docker_sd_configs` inválido en K8s | promtail-config.yaml |

## Referencias

- Investigación: subagent `Researcher` (2026-11-25), sesión `ses_deaa7f630ffeYU4p7pz7K7qfbh`.
- ADR-016 (observabilidad), ADR-015/017 (descubrimiento/config), ADR-020 (hardening).
- `infra/observability/*`, `infra/keycloak/realm-export.json`, `compose.yaml`.
