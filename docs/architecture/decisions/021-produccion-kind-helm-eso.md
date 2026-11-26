# ADR-021 — Preparación para producción: kind, Helm, ESO/Vault, Keycloak HTTPS y smoke test

- **Estado:** Aceptado; Fase 10 cerrada (PR #43, squash `eb5ba92`, 2026-11-26)
- **Fecha:** 2026-11-25
- **Decisores:** Architect
- **Contexto:** Fase 10 del roadmap. Fase 9 cerrada (PR #42, `2bc2e5b`).

## Contexto

CASL hoy se despliega únicamente con `docker compose --profile core --profile observability` y
secretos en claro en `compose.yaml`/`realm-export.json`. Fase 10 exige un despliegue reproducible en
un cluster local (`kind`) **sin secretos de producción en Git** (la credencial dev fija del
Postgres local se documenta como brecha conocida), con Keycloak en HTTPS y base de datos
externa, receptores reales de Alertmanager y un smoke test de arranque completo de los ocho
servicios y de la pila de observabilidad (Prometheus, Grafana, Tempo, Loki y Grafana Alloy),
verificando trazas, logs y métricas correlacionadas.

Las decisiones de esta ADR provienen de la investigación `Researcher` (2026-11-25) y se apoyan en
las versiones oficiales verificadas (tabla al final).

## Decisión

1. **Cluster local: `kind` v0.33.0** con imagen de nodo `kindest/node:v1.37.0@sha256:a1ed56cf…`
   (pinneada por digest) y `extraPortMappings` 80/443 para Ingress. ≥6 GB RAM (8 GB recomendado).
   Docker Desktop 4.x (engine 29.6.x) es el provider; no hay incompatibilidad documentada.

2. **Packaging: Helm 4.3.1** (fallback 3.22.1) con un chart *umbrella* `casl` que incluye subcharts/
   templates para los ocho desplegables y la observabilidad, con `values.yaml` base y
   `values-dev.yaml`/`values-prod.yaml` ya versionados.
   `helm lint`/`helm template` son gates de CI; `helm test` se ejecuta en el smoke test
   manual. (Helm 3.16.x es insuficiente frente a K8s 1.37.)

3. **Secretos: External Secrets Operator v2.15.0 + Vault en modo dev** (chart `external-secrets-2.15.0`
   y `vault-helm` 0.34.1). En Git solo se versionan `SecretStore`/`ExternalSecret` (referencias) y
   los manifiestos de despliegue; los valores se escriben en Vault con `vault kv put` y el
   token de bootstrap (`server.dev.devRootToken`) se crea fuera de Git y se materializa como
   Secret con `kubectl create secret`. Sealed Secrets/SOPS quedan como alternativas
   documentadas pero no son el mecanismo principal.

4. **Keycloak en producción:** imagen pre-augmentada (`infra/k8s/keycloak/Dockerfile`,
   `kc.sh build --db=postgres`) y arranque `start --optimized --import-realm` con
   PostgreSQL 17 externo y HTTPS (certificado montado desde Secret; self-signed en dev,
   Let's Encrypt/cert-manager en producción). Se mantiene HTTP interno en el puerto 8080
   (`KC_HTTP_ENABLED=true`) para que los servicios resuelvan JWKS/token por DNS sin
   depositar certificados en cada pod; el `issuer` JWT sigue siendo la URL HTTPS pública.
   `KEYCLOAK_CLIENT_SECRET` es obligatorio: no hay default `casl-order-secret` y el valor
   real se inyecta desde Vault vía ESO.

5. **Alertmanager:** receptor email a **Mailpit v1.34.0** (SMTP `1025`, UI `8025`, REST API)
   frente al webhook local de la configuración Compose previa. `route`, `group_by`,
   `inhibit_rules` y `send_resolved: true`. En K8s las reglas viven en
   `infra/k8s/observability/prometheusrule.yaml` y la configuración de Alertmanager en
   `infra/charts/casl/values.yaml`; `promtool`/`amtool` se ejecutan en el smoke test manual
   sobre los archivos Compose, no como gate de CI.

6. **Observabilidad en K8s:** charts oficiales de la comunidad (`kube-prometheus-stack` 93.11.0,
   que incluye Grafana; `grafana-community/loki` 21.6.0 y `grafana-community/tempo` 3.2.1)
   + **Grafana Alloy** como colector de logs (sustituye Promtail, que usa
   `docker_sd_configs`). Se reutilizan `alert-rules.yml`, `alertmanager.yml`, `datasources.yml` y
   `casl-overview.json` como ConfigMaps/values, conservando los UIDs `casl-*` y la etiqueta `service`
   en las métricas (relabeled desde labels de pod).

7. **Secret `order → payment`:** `client_credentials` se mantiene, pero el secreto real viene del
   Secret materializado por ESO (rotación en Vault con `refreshInterval`).

8. **Escaneo de IaC:** `trivy config` (Trivy 0.76.2) sobre `helm template` y sobre
   `infra/k8s`, con umbral `CRITICAL,HIGH` y `exit-code 1` en CI. Checkov queda como
   *second opinion*.

   8.1. **Exenciones de IaC:** los subcharts upstream (`kube-prometheus-stack`,
   node-exporter, admission webhooks, Loki/Tempo) requieren `hostNetwork`, `hostPID`,
   montajes del host o ejecución root; no son configurables desde los values sin
   romper el chart. Se registran ocho IDs (`KSV-0009`, `KSV-0010`, `KSV-0014`,
   `KSV-0041`, `KSV-0056`, `KSV-0114`, `KSV-0118`, `KSV-0121`) en `.trivyignore.yaml`
   con `statement` y `expired_at` (`2027-02-24`), y se documentan en
   `docs/security/exceptions.md`. Se reevalúan antes de caducar y con cada revisión
   mayor de los charts. El código CASL propio no usa exenciones: sus manifiestos
   (postgres, Keycloak, Alloy, Mailpit y los ocho desplegables) corrigen los hallazgos.

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

- (+) Despliegue reproducible en `kind` sin secretos de producción en Git; rotación
  centralizada en Vault.
- (−) El Postgres local conserva la credencial dev fija `casl/casl` en
  `infra/k8s/postgres.yaml`; debe externalizarse antes de cualquier entorno no local.
- (+) Observabilidad con los UIDs y la etiqueta `service` preservados → dashboard y alertas
  funcionan sin cambios.
- (+) Escaneo de IaC con la misma herramienta y umbral que el resto del pipeline.
- (−) Coste de RAM: kube-prometheus-stack + Loki/Tempo/Alloy consume mucho más que Compose;
  el smoke test puede requerir 8-10 GB para Docker Desktop.
- (−) Alloy añade una curva de aprendizaje frente a Promtail, pero es el camino soportado.
- (−) `start --optimized` exige construir la imagen de Keycloak antes del despliegue; no hay
  job de CI que la construya (ver `docs/production/README.md` §2b).

## Verificación

**Ejecutada en el PR #43:**

- `helm dependency update`, `helm lint` y `helm template` sin errores.
- `trivy config --severity CRITICAL,HIGH --exit-code 1` sobre el chart renderizado y sobre
  `infra/k8s`: 0 hallazgos CRITICAL/HIGH (con las exenciones de §8.1).
- Parse YAML de los 15 manifiestos de `infra/k8s` sin errores.
- `git diff --check` sin errores.
- Checks del PR en verde: `unit tests`, `integration tests`, `CodeQL`,
  `CodeQL (java-kotlin)`, `sbom + trivy`, `trivy image scan (non-blocking)` y
  `trivy config (helm misconfiguration)`.

**Pendiente de ejecución manual (requiere Docker/kind):**

- `bash scripts/smoke.sh`: creación o reutilización del cluster, `helm upgrade --install --wait`,
  `helm test`, `kubectl wait`, health de los 8 servicios, queries a
  Prometheus/Loki/Tempo, dashboard y correos en Mailpit. El script comprueba como
  prerrequisito los Secret/ConfigMap creados según `docs/production/README.md`.

**Cierre:** PR #43 mergeado por squash el 2026-11-26 como `eb5ba92` en `Master`; rama
`feat/phase-10-prod-prep` eliminada. Sin cambios de código de negocio.

## Versiones mínimas verificadas (2026-11-25)

| Componente | Recomendado | Mínimo | Fuente |
|---|---|---|---|
| Helm | 4.3.1 (fallback 3.22.1) | ≥3.22.x con K8s 1.37 | helm.sh version_skew |
| kind | v0.33.0 | K8s 1.37.0, Docker Desktop 4.x | kind release notes |
| kubectl | 1.37.x | skew estándar | K8s |
| ESO | v2.15.0 | K8s ≥1.16 | external-secrets.io |
| Vault chart | 0.34.1 / 2.0.4 | `audience` desde Vault 1.21 | vault-helm release |
| Keycloak | 26.8.x | PostgreSQL 14–18 (17 incluido) | keycloak.org/server/db |
| Alertmanager | v0.35.0 | `amtool check-config` | prometheus.io |
| Mailpit | v1.34.0 | SMTP 1025 / UI 8025 | release notes |
| Trivy | 0.76.2 (proyecto 0.70.0) | `trivy config` | trivy.dev |
| kube-prometheus-stack | 93.11.0 (op. 0.95.2) | `kubeVersion >=1.25` | Chart.yaml |
| grafana-community/loki | 21.6.0 (app 3.7.10) | paridad con Compose | Chart.yaml |
| grafana-community/tempo | 3.2.1 (app 3.1.1) | paridad con Compose | Chart.yaml |
| Grafana (vía kube-prometheus-stack) | incluido en el stack 93.11.0 | dashboard schemaVersion 39 | chart kube-prometheus-stack |
| Promtail → Alloy | Alloy | `docker_sd_configs` inválido en K8s | promtail-config.yaml |

## Referencias

- Investigación: subagent `Researcher` (2026-11-25), sesión `ses_deaa7f630ffeYU4p7pz7K7qfbh`.
- ADR-016 (observabilidad), ADR-015/017 (descubrimiento/config), ADR-020 (hardening).
- `infra/observability/*`, `infra/keycloak/realm-export.json`, `compose.yaml`.
