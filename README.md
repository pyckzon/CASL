# CASL

Sistema de microservicios de e-commerce construido como portfolio de arquitectura de alta escalabilidad.

## Stack

- Spring Boot 4.1.2 · Java 21 · Gradle multi-módulo (Kotlin DSL)
- Spring Cloud 2025.1.4 (Gateway WebMVC) · Apache Kafka (saga coreografiada + Outbox)
- PostgreSQL + Flyway · Redis · Keycloak 26.8.2
- Micrometer / Prometheus / Grafana / Loki / Tempo (OTLP)

## Estructura

```
services/
  casl-users-service/     # usuarios (en construcción)
gradle/libs.versions.toml # versiones centralizadas
buildSrc/                 # plugin de convención casl-spring-boot-service
docs/architecture/decisions/  # ADRs
compose.yaml              # orquestación por perfiles
```

## Requisitos

- JDK 21 (Temurin recomendado)
- Docker Desktop en ejecución (compose y Testcontainers)

## Comandos

```bash
./gradlew build                        # compilar y verificar todos los módulos
./gradlew :services:casl-users-service:test
./gradlew :services:casl-users-service:bootRun
./gradlew :services:casl-users-service:bootBuildImage   # imagen buildpacks
docker compose --profile core up -d    # PostgreSQL + casl-users-service
```

## Estado

Fase 0 completada: estructura multi-módulo, convenciones de build, compose base y ADRs. La API de usuarios aún es un esqueleto; el rediseño de contratos, persistencia y seguridad llega en las siguientes fases.

Ver `docs/architecture/decisions/` para las decisiones arquitectónicas.
