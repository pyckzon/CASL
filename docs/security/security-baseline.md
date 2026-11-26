# Baseline de seguridad — CASL

> Estado: activo · Última revisión: 2026-11-26 · Responsable: Architect

## 1. Estado verificado

| Verificación | Comando | Resultado |
|---|---|---|
| Vulnerabilidades de la imagen | `trivy image casl/casl-users-service:0.0.1-SNAPSHOT` | **0** (Ubuntu 24.04, ~50 JARs, binarios del buildpack) |
| Secretos en el historial | `gitleaks detect` | **0** en 15 commits |
| Dependabot alerts | GitHub | habilitado |
| Secret scanning + push protection | GitHub | habilitado (repo público) |
| SAST | CodeQL `security-extended` | workflow activo en PR, push a `Master` y semanal |
| SBOM por módulo | `./gradlew cyclonedxDirectBom --no-daemon` | 9/9 JSON con PURLs (`pkg:maven/...`) en `build/reports/sbom/*-bom.json` |
| Trivy sobre SBOM | `trivy sbom --severity CRITICAL,HIGH --ignore-unfixed --ignorefile .trivyignore.yaml` | **0 hallazgos** en los 9 módulos; antes de la remediación: 6 módulos 0, `order`/`notification`/`shipping` 3 HIGH (lz4-java, snappy-java; con fix disponible) |
| JWT dev validado | `curl -H "Authorization: Bearer $tok" http://localhost:8080/api/catalog/v1/products` | **200** (Keycloak 26.8, issuer `http://localhost:8180/realms/casl`, `sub` no vacío) |
| DAST (ZAP AF) | `zap.sh -cmd -autorun .zap/plan.yaml` | 7 specs importados, exit 0, 2xx=13, 0 "Authentication required" en logs de gateway; token en forma segura |
| IaC (Helm + K8s) | `trivy config --severity CRITICAL,HIGH --exit-code 1 --ignorefile .trivyignore.yaml` sobre `helm template` e `infra/k8s` | **0 hallazgos** CRITICAL/HIGH en CI del PR #43; 8 exenciones de subcharts upstream en `exceptions.md` |

Instantánea del 2026-11-21: Spring Boot 4.1.2, Spring Framework 7.0.10, Tomcat 11.0.27, Log4j2 2.25.5, Jackson 3.1.7, Micrometer 1.17.2. El repositorio ya usa Spring Boot 4.2.0 y Spring Cloud 2026.0.0 (PR #38), por lo que esa imagen debe reconstruirse para volver a medir.

Una foto no es un proceso: la vigencia depende de los escaneos automáticos de §2. El estado de las fases está en [`docs/roadmap.md`](../roadmap.md).

## 2. Capas

| Capa | Herramienta | Estado | Cuándo |
|---|---|---|---|
| Secretos en historial | gitleaks | **activo** | manual / pre-release |
| Secretos en push | GitHub secret scanning + push protection | **activo** | automático |
| Dependencias (CVE) | Dependabot alerts + security updates | **activo** | continuo |
| Dependencias en build (SBOM) | CycloneDX (Gradle 3.6.1, `cyclonedxDirectBom`) | **activo** | PR; 9 JSON/módulo |
| SAST | CodeQL `security-extended` | **activo** | PR + push a `Master` + semanal |
| Contenedor | Trivy image | **activo (no bloqueante)** | CI, imagen `casl-users-service` |
| Dependencias en build | OWASP Dependency-Check | **n/a** | No se adopta (Fase 9: SBOM + Trivy + Dependabot ya lo cubren) |
| SBOM de módulo + Trivy sobre SBOM | Trivy 0.70.0 | **activo (no bloqueante)** | CI, `CRITICAL,HIGH`, `ignore-unfixed`, allowlist caducada |
| DAST | OWASP ZAP Automation Framework | **activo (manual, no bloqueante)** | `workflow_dispatch` en `.github/workflows/dast.yml`; plan `.zap/plan.yaml`; lee specs de `postman/specs/` |
| IaC / Helm | Trivy misconfig | **activo (bloqueante)** | PR + push a `Master`; `helm template` e `infra/k8s`, umbral `CRITICAL,HIGH` |

Checkov queda como *second opinion* (ADR-021); las exenciones de subcharts upstream se rigen por `exceptions.md` y caducan el 2027-02-24.

## 3. Ejecución local

```bash
# Imagen (requiere haber corrido bootBuildImage)
docker run --rm -v trivy-cache:/root/.cache \
  -v /var/run/docker.sock:/var/run/docker.sock \
  aquasec/trivy:latest image casl/casl-users-service:0.0.1-SNAPSHOT

# Historial de git
docker run --rm -v "$PWD:/repo" zricethezav/gitleaks:latest \
  detect --source=/repo --redact -v
```

## 4. Política de severidad (SLA de corrección)

| Severidad | Con fix disponible | Sin fix disponible |
|---|---|---|
| Crítica (CVSS ≥ 9.0) | 7 días | Mitigación documentada + revisión semanal |
| Alta (7.0–8.9) | 30 días | Mitigación o excepción aprobada |
| Media (4.0–6.9) | 90 días | Se reevalúa en el siguiente ciclo |
| Baja (< 4.0) | Próxima actualización planificada | — |

## 5. Excepciones

Toda excepción se registra en `docs/security/exceptions.md` con:

- ID del hallazgo (CVE / regla CodeQL / alerta Dependabot).
- Justificación técnica (no alcanzable, requiere configuración no usada, etc.).
- Fecha de expiración (máximo 90 días).
- Contramedida aplicada mientras esté vigente.

Sin fecha de expiración, la excepción no es válida.

## 6. Reglas de código

- DTOs de salida independientes del dominio; nunca se serializa una entidad completa.
- `@ToString.Exclude` en campos sensibles; prohibido loggear payloads completos.
- Errores al cliente como `problem+json` sin stacktrace ni mensajes internos.
- Actuator expone `health,metrics,prometheus` en el puerto management `9090`, no publicado
  al host; la cadena `@Order(1)` de `casl-security` restringe ese puerto.
- Validación de entrada con Bean Validation + límites de tamaño.
- Credenciales solo por variable de entorno o secreto; nunca en `application.yaml` versionado.

## 7. Brechas conocidas

| Brecha | Impacto | Mitigación actual |
|---|---|---|
| Sin DAST (estático) | Los paths mutantes no se proban dinámicamente | Pruebas de slice/integración, revisión de contrato; ZAP AF activo (Fase 9) |
| Credenciales dev en `compose.yaml` | Solo aceptable en local | Realm de desarrollo documentado; en K8s los secretos se materializan desde Vault con ESO (Fase 10), sin secretos de producción en Git (única excepción en la fila siguiente) |
| Sin segmentación de red entre servicios | Un servicio comprometido puede alcanzar otros puertos | Zero-trust JWT en cada servicio y puerto management no publicado |
| Exenciones de IaC en subcharts upstream | RBAC/host mounts amplios en charts de terceros | Ocho IDs con justificación y caducidad en `exceptions.md`; el código CASL propio no usa exenciones |
| Credencial dev fija en `infra/k8s/postgres.yaml` | Solo apta para el cluster local `kind` | Documentada en `docs/production/README.md` y ADR-021; externalizar desde Vault antes de cualquier entorno no local |
