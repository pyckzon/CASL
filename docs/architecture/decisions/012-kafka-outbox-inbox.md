# ADR-012 — Event-driven con Kafka, Transactional Outbox e idempotencia de consumidores

- **Estado:** Aceptado
- **Fecha:** 2026-11-22
- **Decisores:** Architect

## Contexto

La saga síncrona (Fase 3) acopla temporalmente order↔payment. Para escalar y desacoplar, la comunicación entre dominios debe ser asíncrona y confiable: un evento de negocio no debe perderse ni duplicarse, y los consumidores deben ser idempotentes.

## Decisión

1. **Kafka (single-node, KRaft)** como broker. Topics versionados: `user.created.v1`, `order.created.v1`, `order.cancelled.v1`, `payment.approved.v1`, `payment.rejected.v1`, `stock.reserved.v1`, `stock.released.v1`, `shipment.dispatched.v1`, `shipment.delivered.v1`.
2. **Transactional Outbox.** Cada servicio persiste el evento en una tabla `outbox` dentro de la misma transacción de negocio. Un relay (scheduler) publica los eventos pendientes a Kafka y los marca como publicados. Esto evita "evento a medias" si la transacción falla.
3. **Envelope de evento.** Todos los eventos comparten un envelope: `eventId` (UUID), `eventType`, `aggregateId`, `occurredAt`, `schemaVersion`, `traceId`, `payload` (JSON). Los consumidores usan `eventId` para idempotencia (inbox).
4. **Consumidores idempotentes (inbox).** Cada consumidor persiste el `eventId` procesado en una tabla `inbox`; si ya existe, ignora el evento. Esto protege contra redelivery y reprocesamiento.
5. **Saga coreografiada.** Los servicios reaccionan a eventos y emiten los suyos: `order.created` → catalog reserva stock → `stock.reserved` → payment cobra → `payment.approved` → shipping despacha → `shipment.dispatched` → notification notifica. Compensación: `payment.rejected` → `stock.released` + `order.cancelled`.
6. **Particionado por `aggregateId`.** Garantiza orden de eventos por agregado.
7. **notification-service** consume eventos y despacha por canal (Strategy: log, email simulado, webhook). Añadir un canal es añadir una implementación.

## Consecuencias

- (+) Desacoplamiento temporal: los servicios no se bloquean mutuamente.
- (+) Confiabilidad: outbox garantiza que el evento se publica si la transacción se confirma.
- (+) Idempotencia: inbox evita procesamiento duplicado.
- (+) Escalabilidad: consumidores se escalan por partición.
- (−) Complejidad operativa (broker, outbox relay, inbox).
- (−) Consistencia eventual: la orden puede tardar milisegundos en reflejar el pago.

## Alternativas consideradas

- **RabbitMQ:** más simple para routing, pero Kafka es el estándar para event sourcing y replay. Se elige Kafka por la capacidad de replay y el portfolio.
- **Publicación directa (sin outbox):** riesgo de perder eventos si el servicio cae entre commit y publish.
- **Saga orquestada:** centraliza el flujo pero añade un componente extra; coreografía es suficiente para este tamaño.
- **Kafka sin idempotencia:** riesgo de procesamiento duplicado ante redelivery.
