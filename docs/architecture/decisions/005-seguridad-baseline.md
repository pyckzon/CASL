# ADR-005 — Seguridad: CodeQL como SAST principal y escaneo por capas diferido

- **Estado:** Aceptado
- **Fecha:** 2026-11-21
- **Decisores:** Architect (input de DevOps y Reviewer)

## Contexto

El repositorio es público y el stack es un objetivo realista de CVEs transitivos (Spring Boot, Tomcat, Jackson, Log4j2, y más adelante Kafka/Keycloak/Netty). La línea base del 2026-11-21 está limpia: Trivy sobre la imagen reportó 0 vulnerabilidades y gitleaks 0 secretos en 15 commits. Eso no se sostiene sin escaneo continuo.

Restricciones:

- Stack 100% gratuito.
- El pipeline no debe volverse lento ni ruidoso: un gate que bloquea por CVEs heredados sin fix termina desactivado.
- El proyecto aún es un esqueleto; el volumen de código propio es mínimo.

## Decisión

1. **SAST principal: CodeQL** (`security-extended`), en PR, push a `Master` y semanal. Gratuito por ser repo público.
2. **Dependabot** con alertas y security updates habilitados, agrupando PRs por familia para no saturar.
3. **Secret scanning + push protection** de GitHub habilitados (gratis en público) más gitleaks para el historial.
4. **Diferir** Trivy image, OWASP Dependency-Check, SBOM CycloneDX y OWASP ZAP hasta Fase 2–3, cuando haya código y contrato que analizar. Antes de activarlos se mide su duración y su tasa de falsos positivos.
5. **Gate gradual**: primero report-only; después bloqueante en `CRITICAL`/`HIGH` con allowlist documentada y fecha de expiración.

## Alternativas consideradas

- **Snyk Free**: cubre SCA/SAST/IaC/contenedor, pero con 5 proyectos y ~200 tests SCA/mes. Sirve como segunda opinión, no como pipeline principal.
- **OWASP Dependency-Check desde el día 1**: requiere API key de NVD y la primera descarga tarda decenas de minutos; desproporcionado para un esqueleto.
- **Solo Dependabot**: barato, pero no analiza código propio ni configuraciones.

## Consecuencias

- (+) Cero coste y cero mantenimiento de infraestructura.
- (+) Cobertura inmediata de dependencias y de código propio.
- (+) El pipeline se mantiene rápido mientras el proyecto es pequeño.
- (−) Trivy y ZAP no cubren todavía contenedor ni runtime; queda documentado como brecha en `docs/security/security-baseline.md`.
- (−) CodeQL `security-extended` puede generar hallazgos de baja prioridad en código nuevo; se gestionan por la política de SLA.
- Revisión obligatoria: al cerrar Fase 3 se reevalúa activar Trivy + Dependency-Check + SBOM y se registra la medición.

## Actualización — cierre de Fase 6 (2026-11-24)

- **Trivy image** quedó activo en Fase 2.3 como job *no bloqueante* de CI, sobre la
  imagen `casl/casl-users-service`. Se mantiene report-only hasta definir la allowlist.
- **CodeQL**, **Dependabot**, el *secret scanning* de GitHub y gitleaks siguen activos.
- **SBOM CycloneDX**, **DAST con OWASP ZAP** y la decisión sobre **OWASP Dependency-Check**
  se trasladan a Fase 9; el escaneo de IaC queda en Fase 10, cuando existan manifiestos.
- El detalle de alcance y criterios de aceptación está en `docs/roadmap.md`.
