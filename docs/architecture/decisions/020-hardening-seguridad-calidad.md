# ADR-020 — Hardening de seguridad y calidad: SBOM, Trivy y DAST autenticado

- **Estado:** Aceptado
- **Fecha:** 2026-11-24
- **Decisores:** Architect

## Contexto

La Fase 9 del roadmap cierra los controles que `docs/security/security-baseline.md`
mantenía diferidos: SBOM por módulo, escaneo de dependencias del build, DAST y la
reevaluación de OWASP Dependency-Check. El repositorio ya tiene CodeQL, Dependabot,
secret scanning, gitleaks y un escaneo Trivy de imagen no bloqueante, pero no genera
SBOM ni analiza el grafo de dependencias de cada módulo.

El gateway es una API sin HTML navegable. Un `zap-baseline.py` puro descubre pocas o
ninguna URL y produciría una falsa sensación de cobertura. Además, los servicios
reciben JWT de Keycloak y el stack local ya expone los siete contratos OpenAPI 3.1.

Durante la validación del stack se detectó que solo `users`, `catalog` y `cart`
recibían `KEYCLOAK_JWK_URI` interno. Los demás servicios intentaban descargar JWKS
desde `http://localhost:8180` dentro del contenedor, por lo que rechazaban tokens
válidos con `401`. Es un defecto de infraestructura, no de seguridad del dominio.

## Decisión

1. **CycloneDX por módulo con el plugin oficial de Gradle.** Se usa
   `org.cyclonedx.bom` 3.6.1, aplicado una sola vez en la raíz y configurado desde la
   convención `casl-spring-boot-service`. Cada módulo genera únicamente JSON en
   `build/reports/sbom/<módulo>-bom.json`, con `failOnUnresolvedDependencies = true`.
   Se mantiene CycloneDX 1.6, el formato por defecto y el soportado por Trivy.
2. **Trivy sobre SBOM, no solo sobre imagen.** El CI instala Trivy 0.70.0 con
   `aquasecurity/setup-trivy@v0.3.1` y ejecuta `trivy sbom` sobre los nueve SBOM con
   `CRITICAL,HIGH`, `ignore-unfixed` y `exit-code 1`. El job empieza en modo
   *report-only* (`continue-on-error`) y se promueve a bloqueante cuando la línea base
   de ruido y duración sea aceptable.
3. **Allowlist con caducidad.** Toda excepción de Trivy se registra en
   `.trivyignore.yaml` con `id`, `statement` y `expired_at` (máximo 90 días). Sin
   `expired_at`, la excepción no es válida. El mismo criterio de
   `docs/security/exceptions.md` aplica a las excepciones de ZAP.
4. **DAST con ZAP Automation Framework autenticado.** Se rechaza el baseline puro para
   este gateway. Se descarta también `zap-api-scan.py` con `ZAP_AUTH_HEADER*`: **esas
   variables solo existen en los scripts baseline/full**, no en el API scan, y la
   ejecución local devolvió el 100 % de las respuestas en 4xx (todas sin token). Se
   adopta un plan del Automation Framework (`.zap/plan.yaml`) con contexto limitado a
   `http://<host>:8080/api/.*`, un job `replacer` que añade `Authorization: Bearer`,
   un job `openapi` por contrato, `passiveScan-wait` y jobs `report`.
   - El token se obtiene en runtime con password grant del realm dev, se enmascara con
     `::add-mask::` y se inyecta en una copia del plan que se borra al terminar; nunca
     se versiona ni se pasa por línea de comandos.
   - Las variables `${var}` del plan **no** se sustituyen en `replacementString` (medido:
     seguía devolviendo 4xx), por eso la inyección es textual sobre la copia temporal.
   - El importador OpenAPI del AF es más estricto que el de `zap-api-scan.py`: exige
     `description` en cada respuesta. Se corrigieron los siete contratos antes de
     habilitar el plan.
   - Fase observacional: solo escaneo pasivo, job manual (`workflow_dispatch`) y
     `continue-on-error`. El workflow captura `zap_exit_code` y lo resume.
   - El plan termina con job `exitStatus` (`okExitValue: 0`, `warnExitValue: 2`),
     porque el default de ZAP devuelve 2 con warnings y sin él ninguna alerta
     afecta al código de salida.
5. **No se activa OWASP Dependency-Check.** Dependabot ya cubre Gradle, el version
   catalog, GitHub Actions y Docker; SBOM + Trivy cubren los CVE del grafo. Activar
   Dependency-Check añadiría NVD API key/mirror, caché H2, OSS Index y un formato de
   supresiones separado sin mejorar la cobertura de forma proporcional. Se reevaluará
   solo si aparece un requisito de compliance que exija sus reportes.
6. **Corrección de infraestructura.** Todos los servicios de `compose.yaml` reciben
   `KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/casl` para validar el `iss` del
   token emitido al host y `KEYCLOAK_JWK_URI=http://keycloak:8080/realms/casl/protocol/openid-connect/certs`
   para resolver JWKS por la red interna.

## Alternativas rechazadas

- **Aplicar CycloneDX en cada `build.gradle.kts`:** duplica tareas y pierde el agregado
  de raíz; la convención ya es el punto único de configuración.
- **Generar SBOM solo desde la imagen:** cambia el alcance a imagen y no cumple el
  requisito de SBOM por módulo Gradle.
- **OWASP Dependency-Check ahora:** alto coste operativo y solapamiento con
  Dependabot + SBOM + Trivy.
- **ZAP baseline puro:** no descubre las rutas de una API; el API scan usa los
  contratos existentes y permite limitar el objetivo al gateway.
- **Publicar reportes ZAP completos:** pueden contener `Authorization`; se redactan o
  se omiten como artefactos.

## Consecuencias

- (+) Hay SBOM reproducible por módulo y un escaneo de dependencias del build.
- (+) DAST cubre los contratos reales y no solo HTML enlazable.
- (+) Las excepciones de seguridad caducan y quedan auditables.
- (−) El job de SBOM/Trivy añade tiempo al pipeline.
- (−) El job ZAP requiere stack completo y secretos dev configurados; por eso empieza
  manual y no bloqueante.
- (−) Los SBOM generados por Gradle pueden tener menor precisión en Trivy que los
  generados por la propia herramienta; se validará contra el escaneo de imagen antes
  de bloquear.

## Verificación

Pendiente de completar durante la implementación:

- `./gradlew cyclonedxDirectBom --no-daemon` genera nueve JSON con PURLs.
- `trivy sbom` sobre cada JSON respeta severidad, `ignore-unfixed` y allowlist.
- El stack `core` autentica un JWT de `admin` contra el gateway con `200`
  (verificado: `catalog` y `users/me` responden 200).
- ZAP API scan se ejecuta contra el gateway, importa URLs y no filtra el token.
- `unit tests`, `integration tests` y CodeQL siguen en verde.
