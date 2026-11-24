# ADR-016 — Observabilidad: trazas OTLP, logs correlacionados y métricas

- **Estado:** Aceptado
- **Fecha:** 2026-11-23
- **Decisores:** Architect

## Contexto

Las ADR-002, -011, -013 y -014 prevén trazas OTLP/Tempo, logging estructurado con
`traceId`, y alertas básicas de circuit breaker, lag de Kafka y DLT para la Fase 6.
Actualmente los servicios exponen Actuator en el puerto de aplicación, solo
`users`/`catalog`/`order` publican `prometheus`, no hay bridge de trazas y los logs son
de texto plano (con enmascaramiento PII propio en `users`). El perfil `observability`
de `compose.yaml` está reservado pero vacío.

## Decisión

1. **Trazas.** Todos los desplegables (siete servicios + gateway) incluyen
   `org.springframework.boot:spring-boot-starter-opentelemetry`. Boot 4.2.0 gestiona
   `micrometer-tracing-bridge-otel` 1.8.0 y `opentelemetry-exporter-otlp` 1.68.0
   (verificado con `dependencyInsight` tras el PR #38).
   Configuración: `management.tracing.sampling.probability` (por defecto `0.1`,
   `1.0` en desarrollo) y
   `management.opentelemetry.tracing.export.otlp.endpoint=${OTLP_TRACES_ENDPOINT:http://localhost:4318/v1/traces}`
   con transport `http` (valor por defecto). La propagación HTTP usa los builders
   auto-configurados (`RestClient`, `RestTemplate`, `WebClient`); no se crean clientes
   a mano. No se añade Zipkin.
2. **Correlación de logs.** Log4j2 usa `JsonTemplateLayout` con un event template
   compartido que emite `traceId` y `spanId` desde MDC, además de `timestamp`, `level`,
   `logger`, `service`, `thread` y `message`. En `users` se mantiene el enmascaramiento
   de PII mediante un resolver JSON propio que delega en `PiiMasker`; el converter
   `%pii` del `PatternLayout` se conserva para ejecución local legible si se desea.
3. **Superficie de métricas.** Se habilitan `health,metrics,prometheus` en los ocho
   desplegables. Actuator se sirve en un puerto management separado
   (`management.server.port`, por defecto 9090, **no publicado al host** en Compose).
   `CaslSecurityConfiguration` añade una cadena `@Order(1)` restringida a ese puerto
   que permite solo los endpoints de Actuator y aplica `denyAll` al resto; la cadena
   `@Order(2)` mantiene el deny-by-default actual para el puerto de aplicación.
   Los ocho servicios desplegables importan esta configuración, incluidos
   `notification` y `shipping`.
4. **Alertas y stack local.** Perfil `observability` en Compose con Prometheus
   v3.16.0, Alertmanager v0.35.0, Grafana 13.2.6, Tempo 3.1.1, Loki 3.7.10 y
   Promtail 3.6.11. Reglas: error rate 5xx, circuit breaker abierto, lag de Kafka y
   publicaciones a DLT. Provisioning de datasources y un dashboard mínimo incluidos.
   Alertmanager usa un receptor webhook local sin consumidor en dev; las integraciones
   reales de notificación se deciden en Fase 10.
5. **Métrica de DLT.** `notification` y `shipping` envuelven el
   `DeadLetterPublishingRecoverer` con un `ConsumerRecordRecoverer` que incrementa
   `casl.dlt.published` (tag `service`), base de la cuarta alerta.

## Alternativas rechazadas

- Exponer `/actuator/prometheus` en el puerto de aplicación con `permitAll`: rompe el
  principio zero-trust y deja métricas accesibles desde la red de servicios.
- Zipkin/Brave: el ecosistema ya elegido es OTLP/Tempo (ADR-002).
- Agente OTel externo (javaagent): añade otro artefacto y complica buildpacks; el
  starter de Boot cubre las necesidades actuales.

## Consecuencias

- (+) Trazas, logs y métricas comparten `traceId`/`spanId` de extremo a extremo.
- (+) El puerto de scraping no es alcanzable desde el host ni desde la API.
- (−) Hay que mantener reglas, dashboard y provisioning versionados en `infra/`.
- (−) El enmascaramiento cubre emails y teléfonos etiquetados; otros datos personales
  no se infieren de forma genérica y deben evitarse en mensajes de log.
- (−) Los `@SpringBootTest` no auto-configuran exportadores de trazas (comportamiento
  documentado de Boot 4); las pruebas se centran en configuración y serialización.

## Verificación

- `./gradlew.bat test`, `compileIntegrationTestJava`, `jacocoTestCoverageVerification`.
- `docker compose --profile core --profile observability config` (sin daemon).
- `promtool check rules/config` y `amtool check-config` para reglas y receptores.
- `tempo -config.verify`, `loki -verify-config` y `promtail -check-syntax`.
- Revisión independiente y checks de PR antes de squash merge en `Master`.
