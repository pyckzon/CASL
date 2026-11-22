# ADR-013 — Saga coreografiada y shipping-service

- **Estado:** Aceptado
- **Fecha:** 2026-11-22
- **Decisores:** Architect

## Contexto

La saga síncrona (Fase 3) acopla temporalmente order↔payment y no escala. La Fase 4 introduce comunicación asíncrona: los servicios reaccionan a eventos y emiten los suyos (coreografía). El shipping-service es el último paso de la saga de compra.

## Decisión

1. **Saga coreografiada.** No hay orquestador central. Cada servicio reacciona a eventos y emite los suyos:
   - `order.created.v1` → catalog reserva stock → `stock.reserved.v1`
   - `stock.reserved.v1` → payment cobra → `payment.approved.v1` / `payment.rejected.v1`
   - `payment.approved.v1` → shipping despacha → `shipment.dispatched.v1`
   - `shipment.dispatched.v1` → notification notifica
   - Compensación: `payment.rejected.v1` → catalog libera stock (`stock.released.v1`) + order cancela (`order.cancelled.v1`)
2. **shipping-service** (puerto 8675): cotización de envío, despachos y tracking. Persiste `Shipment` en PostgreSQL. Consume `payment.approved.v1`, crea el envío y publica `shipment.dispatched.v1`.
3. **ShippingProvider como Strategy.** El cálculo de tarifa se delega a `ShippingProvider` (implementación inicial: tarifa fija por peso). Añadir transportistas reales no modifica el dominio (OCP, §2.3).
4. **Idempotencia.** El consumer de `payment.approved.v1` es idempotente (inbox): si el envío ya existe para esa orden, no se duplica.
5. **Compensación por eventos.** Si el pago falla, el order-service publica `order.cancelled.v1`; catalog libera stock. No se requiere orquestador.

## Consecuencias

- (+) Desacoplamiento total: cada servicio evoluciona independientemente.
- (+) Compensación automática por eventos.
- (+) Escalabilidad: consumidores se escalan por partición.
- (−) Flujo distribuido más difícil de depurar (mitigado por trazas OTLP en Fase 6).
- (−) Consistencia eventual: la orden puede tardar milisegundos en reflejar el envío.

## Alternativas consideradas

- **Saga orquestada (orchestrator):** centraliza el flujo pero añade un componente extra y un punto de fallo.
- **Saga síncrona (Fase 3):** más simple pero acopla temporalmente los servicios.
- **Shipping como parte de order-service:** viola database-per-service y bounded context.
