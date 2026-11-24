# CASL

Sistema de microservicios de e-commerce construido como portfolio de arquitectura de alta escalabilidad.

## Stack

- Spring Boot 4.2.0 · Java 21 · Gradle 9.9.2 multi-módulo (Kotlin DSL)
- Spring Cloud 2026.0.0 (Gateway WebMVC) · Apache Kafka (saga coreografiada + Outbox)
- PostgreSQL + Flyway · Redis · Keycloak 26.8
- Micrometer/OTLP/Tempo · Prometheus · Grafana · Loki · Promtail

## Estructura

```
services/
  casl-gateway/              # ruteo y validación JWT
  casl-security/             # módulo compartido de zero-trust
  casl-users-service/        # usuarios y aprovisionamiento JIT
  casl-catalog-service/      # productos, categorías y stock
  casl-cart-service/         # carrito en Redis
  casl-order-service/        # orden, saga y resiliencia
  casl-payment-service/      # pagos idempotentes
  casl-notification-service/ # consumidor de eventos
  casl-shipping-service/     # saga coreografiada de envío
postman/specs/               # contratos OpenAPI 3.1
infra/observability/         # Prometheus, Alertmanager, Grafana, Tempo, Loki, Promtail
infra/keycloak/              # realm export de desarrollo
docs/architecture/decisions/ # ADRs
docs/roadmap.md              # fases pendientes y criterios de aceptación
compose.yaml                 # orquestación por perfiles
buildSrc/                    # plugin de convención casl-spring-boot-service
gradle/libs.versions.toml    # versiones centralizadas
```

## Requisitos

- JDK 21 (Temurin recomendado)
- Docker Desktop en ejecución (perfiles de Compose y Testcontainers)
- El wrapper incluido (`gradlew` / `gradlew.bat`); no requiere Gradle instalado

## Comandos

```bash
./gradlew build                                        # compilar y verificar todos los módulos
./gradlew test                                         # suite unitaria
./gradlew :services:casl-users-service:bootRun
./gradlew :services:casl-users-service:bootBuildImage  # imagen buildpacks
docker compose --profile core up -d                    # stack funcional
docker compose --profile core --profile observability up -d  # + métricas, trazas y logs
```

Los endpoints de Actuator (`health`, `metrics`, `prometheus`) se sirven en el puerto
management `9090`, que no se publica al host. El puerto de aplicación devuelve 404 o 401
según la cadena de seguridad.

## Estado

Fases 0 a 6 completadas: API hexagonal, saga síncrona y asíncrona, zero-trust con JWT y
observabilidad correlacionada. No hay PRs abiertos.

- Roadmap y trabajo pendiente: [`docs/roadmap.md`](docs/roadmap.md)
- Decisiones arquitectónicas: [`docs/architecture/decisions/`](docs/architecture/decisions/)
- Línea base de seguridad: [`docs/security/security-baseline.md`](docs/security/security-baseline.md)
- Excepciones vigentes: [`docs/security/exceptions.md`](docs/security/exceptions.md)
