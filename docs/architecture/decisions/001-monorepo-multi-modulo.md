# ADR-001 — Monorepo Gradle multi-módulo con convenciones en `buildSrc`

- **Estado:** Aceptado
- **Fecha:** 2026-11-20
- **Decisores:** Architect, DevOps

## Contexto

El repositorio contenía un único proyecto Spring Boot (`portfolio`) con el código en la raíz (`src/`). El plan del sistema exige ~10 microservicios (`casl-*`) que compartan toolchain, logging, cobertura y empaquetado. Mantener un módulo por repositorio o copiar configuración en cada `build.gradle.kts` produciría duplicación y deriva de versiones.

## Decisión

1. Un **monorepo Gradle multi-módulo**:
   - raíz `casl` (`settings.gradle.kts`), módulos en `services/<servicio>` (`include(":services:casl-users-service")`).
   - Código base en `com.casl.<dominio>`; cada servicio tiene su propio paquete y `*Application`.
2. **Version catalog** en `gradle/libs.versions.toml` para versiones de dependencias y plugins.
3. **Plugin de convención** `casl-spring-boot-service` en `buildSrc`, que aplica Java, Spring Boot, dependency-management, JaCoCo, exclusión global de `spring-boot-starter-logging`, Lombok y configuración de tests.
4. **Un comando para todo**: `./gradlew build` construye y verifica todos los módulos.

## Consecuencias

- (+) Configuración centralizada; un cambio de versión o de gate aplica a todos los servicios.
- (+) CI actual (`./gradlew check`) sigue siendo válido para el monorepo.
- (+) Migración de `portfolio` → `casl-users-service` sin pérdida de historial ni del código existente.
- (+) La toolchain Java 21 se aplica desde la convención (import explícito de `JavaLanguageVersion` en el script precompilado), sin duplicarla en cada servicio.
- (−) Los starters de Spring Boot llevan `version.ref` en el catálogo (Gradle 9.8.1 no admite `[platforms]` en el TOML); el BOM de Boot sigue gestionando las versiones transitivas.
- (−) `@WebMvcTest` en Spring Boot 4 requiere `spring-boot-starter-webmvc-test` explícito (no viene en `spring-boot-starter-test`).

## Alternativas consideradas

- **Un repositorio por servicio:** aísla equipos, pero multiplica builds, CI y versiones; coste desproporcionado para un portfolio.
- **Configuración duplicada por módulo (sin `buildSrc`):** simple al inicio, insostenible con 10 servicios.
