# Baseline de seguridad — CASL

> Estado: activo · Última revisión: 2026-11-21 · Responsable: Architect

## 1. Estado verificado

| Verificación | Comando | Resultado |
|---|---|---|
| Vulnerabilidades de la imagen | `trivy image casl/casl-users-service:0.0.1-SNAPSHOT` | **0** (Ubuntu 24.04, ~50 JARs, binarios del buildpack) |
| Secretos en el historial | `gitleaks detect` | **0** en 15 commits |
| Dependabot alerts | GitHub | habilitado |
| Secret scanning + push protection | GitHub | habilitado (repo público) |
| SAST | CodeQL `security-extended` | workflow activo en PR + semanal |

Versiones observadas en la imagen: Spring Boot 4.1.2, Spring Framework 7.0.10, Tomcat 11.0.27, Log4j2 2.25.5, Jackson 3.1.7, Micrometer 1.17.2.

Una foto no es un proceso: la vigencia depende de los escaneos automáticos de §2.

## 2. Capas

| Capa | Herramienta | Estado | Cuándo |
|---|---|---|---|
| Secretos en historial | gitleaks | **activo** | manual / pre-release |
| Secretos en push | GitHub secret scanning + push protection | **activo** | automático |
| Dependencias (CVE) | Dependabot alerts + security updates | **activo** | continuo |
| SAST | CodeQL `security-extended` | **activo** | PR + push a `Master` + semanal |
| Contenedor | Trivy image | **diferido** | Fase 2–3 (medir coste en CI) |
| Dependencias en build | OWASP Dependency-Check | **diferido** | Fase 2–3 (requiere NVD key) |
| SBOM | CycloneDX (Gradle) | **diferido** | Fase 2–3 |
| DAST | OWASP ZAP baseline/API | **diferido** | Fase 5–6 (necesita Keycloak + contrato) |
| IaC / Helm | Trivy misconfig / Checkov | **diferido** | Fase 10 |

Criterio de activación de lo diferido: medir duración del job y ruido antes de volverlo bloqueante.

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
- Actuator expone únicamente `health` (`management.endpoints.web.exposure.include=health`).
- Validación de entrada con Bean Validation + límites de tamaño.
- Credenciales solo por variable de entorno o secreto; nunca en `application.yaml` versionado.

## 7. Brechas conocidas

| Brecha | Impacto | Mitigación actual |
|---|---|---|
| Sin escaneo de dependencias en el build (no hay lockfiles) | CVE en dependencia nueva se detecta solo al construir imagen o por Dependabot | Dependabot activo; lockfiles en Fase 2–3 |
| Sin DAST | Endpoints no probados dinámicamente | Tests de slice + revisión de contrato |
| Credenciales dev en `compose.yaml` | Solo aceptable en local | Documentado; secretos reales en Fase 5+ |
| CodeQL no analiza el dominio de negocio aún (esqueleto) | Cobertura real baja hasta Fase 1 | Se activa con el código nuevo |
