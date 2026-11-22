# ADR-014 — Idempotencia de consumidores, particionado y métricas de lag

- **Estado:** Aceptado
- **Fecha:** 2026-11-22
- **Decisores:** Architect

## Contexto

La saga coreografiada (Fase 4.2) depende de eventos Kafka. Los consumidores deben ser idempotentes (no procesar el mismo evento dos veces), los eventos deben llegar en orden por agregado, y se necesita observabilidad del lag para detectar cuellos de botella.

## Decisión

1. **Inbox por `eventId`.** Cada consumidor (notification, shipping) persiste el `eventId` procesado en una tabla `inbox` con restricción única. Si el evento ya existe, se ignora. Esto protege contra redelivery y reprocesamiento.
2. **Particionado por `aggregateId`.** Los productores (outbox relay en order-service) usan `aggregateId` como clave de partición (`kafkaTemplate.send(topic, aggregateId, payload)`). Garantiza orden de eventos por agregado.
3. **Métricas de consumer lag.** Spring Kafka expone métricas de consumidor vía Micrometer (`kafka.consumer.*`): `records-lag-max`, `records-consumed-rate`, `fetch-latency-avg`. Se habilitan con `management.metrics.enable.kafka=true` y se exponen en `/actuator/prometheus`.
4. **Tolerant reader.** Los consumidores ignoran campos desconocidos (`spring.json.use.type.headers=false` + `trusted.packages`). Añadir un campo a un evento no rompe consumidores existentes.
5. **Dead letter topic (DLT).** Los mensajes que fallan tras reintentos se envían a `{topic}.DLT` para inspección manual. Configurado con `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`.

## Consecuencias

- (+) Consumidores idempotentes: no se duplican notificaciones ni envíos.
- (+) Orden garantizado por agregado.
- (+) Observabilidad del lag y throughput vía Prometheus/Grafana.
- (+) Tolerant reader permite evolución de eventos sin breaking changes.
- (−) El inbox añade una escritura por evento; aceptable para el volumen del portfolio.
- (−) DLT requiere monitoreo manual; alertas básicas en Fase 6.

## Alternativas consideradas

- **Idempotencia por estado (no inbox):** menos robusto ante redelivery.
- **Particionado por `eventId`:** rompe el orden por agregado.
- **Kafka Streams para agregación:** complejidad prematura; consumers simples son suficientes.
- **Sin DLT (descartar mensajes fallidos):** pérdida de datos; inaceptable.
