# ADR-002 — Stack base y decisiones transversales

- **Estado:** Aceptado
- **Fecha:** 2026-11-20
- **Decisores:** Architect (con insumos de Researcher, DevOps y Reviewer)

## Contexto

El sistema se construirá por fases (ver plan maestro). Antes de escribir features se fijan las versiones y los patrones transversales para evitar retrabajo y deriva de dependencias.

## Decisiones

| Área | Decisión | Motivo / restricción |
|---|---|---|
| Plataforma | Spring Boot **4.2.0**, Java **21**, Gradle **9.9.2** | Versión estable verificada; wrapper del repo compatible |
| Spring Cloud | BOM **2026.0.0** | Train estable compatible con Boot 4.2.0; se descartan release trains RC |
| Gateway | **WebMVC** (`spring-cloud-starter-gateway-server-webmvc`) | Coherente con el stack servlet; evita dependencias reactivas no deseadas |
| Mensajería | **Apache Kafka** + Transactional Outbox | Eventos de dominio, integridad por agregado |
| Saga de compra | **Coreografiada** | Cada servicio reacciona y emite el siguiente evento; sin orquestador central |
| Persistencia | PostgreSQL por servicio + **Flyway** | Database-per-service |
| Caché | Redis | Lecturas de catálogo y carrito |
| Seguridad | **Keycloak 26.8** como IdP; microservicios como **resource servers** (`issuer-uri`), sin adaptadores | Estándar de facto en e-commerce; adaptadores eliminados desde v22 |
| Resiliencia | Resilience4j **2.4.0** (`resilience4j-spring-boot4`) | Circuit breaker, retry, timeouts en llamadas síncronas |
| Observabilidad | Micrometer + Prometheus + Grafana + Loki + **OTLP/Tempo**; `health,metrics,prometheus` en puerto management `9090` | Stack gratuito; Zipkin descartado como backend principal |
| Empaquetado | **Buildpacks** (`bootBuildImage`) vía convención | Sin Dockerfile por servicio; imágenes reproducibles |
| Orquestación | `compose.yaml` raíz con perfiles **core / observability**; sin `spring-boot-docker-compose` en servicios | Evita colisiones de compose en multi-módulo y limita el consumo de RAM en local |
| Cobertura | Gate **50% LINE total**; convención **70% en código nuevo** | Reemplaza el umbral heredado del 20% y las exclusiones de `models/**` |
| Logging | **Log4j2** (se preserva), JSON estructurado y `traceId` en fase de observabilidad | Decisión previa del repositorio |

### Actualización — 2026-11-24

La decisión original fijó **Spring Boot 4.1.2**, **Gradle 9.8.1** y
**Spring Cloud 2025.1.4 (Oakwood)**, incompatible con Boot 4.2.x. Los PR #36 y #38
actualizaron esos valores a los vigentes en la tabla: **Spring Boot 4.2.0**,
**Gradle 9.9.2** y **Spring Cloud 2026.0.0**, según `gradle/libs.versions.toml` y
`gradle/wrapper/gradle-wrapper.properties`. La fila de observabilidad quedó materializada
en Fase 6: `health,metrics,prometheus` se sirven en el puerto management `9090`, no
publicado al host (ADR-016).

## Consecuencias

- Las versiones de dependencias y plugins se consultan en `gradle/libs.versions.toml`; no se fijan versiones ad-hoc en módulos.
- Cualquier cambio de release train o de IdP requiere un ADR nuevo.
- El gate de cobertura se aplica por módulo a través del plugin de convención.
- Los perfiles de compose permiten levantar solo lo necesario (p. ej. `docker compose --profile core up -d`).

## Pendiente resuelto

- ~~Config Server + Eureka (Fase 7) se decide al llegar, según el peso del portfolio.~~
  Resuelto en ADR-017: no se introducen Eureka ni Config Server; Compose por nombre de
  servicio en local y Kubernetes Services + DNS en Fase 10.
- ~~Estrategia de particionado Kafka por agregado (Fase 4).~~ Resuelta en Fase 4.3 y
  documentada en ADR-014 (`docs/architecture/decisions/014-idempotencia-particionado-metricas.md`).

El trabajo restante está en [`docs/roadmap.md`](../../roadmap.md).
