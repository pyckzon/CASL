# ADR-009 — Saga síncrona order→payment con Resilience4j y idempotencia

- **Estado:** Aceptado
- **Fecha:** 2026-11-21
- **Decisores:** Architect

## Contexto

La compra requiere coordinar orden y pago. En esta fase la saga es **síncrona** (REST directo order→payment) con compensación por cancelación. La asincronía (Kafka, outbox) se introduce en Fase 4. Se necesita resiliencia ante fallos del servicio de pago y garantía de no cobrar dos veces.

## Decisión

1. **Saga coreografiada simple (síncrona).** `order-service` crea la orden en estado `PAYMENT_PENDING`, llama a `payment-service` para cobrar y, según el resultado, marca la orden `PAID` o `PAYMENT_FAILED`/`CANCELLED`. La compensación (liberar stock) se dispara al fallar el pago.
2. **Resilience4j** protege la llamada a payment: `@CircuitBreaker`, `@Retry` (backoff exponencial), `@TimeLimiter` (timeout) y `@Bulkhead`. Los fallbacks marcan la orden como `PAYMENT_FAILED` sin propagar el error al cliente como 500.
3. **Idempotencia de pagos.** `payment-service` exige una `Idempotency-Key` (UUID de la orden) y persiste el resultado por clave: repetir la petición devuelve el mismo resultado sin cobrar de nuevo. Tabla `payments` con restricción única sobre `idempotency_key`.
4. **Adapter simulado.** El `PaymentGateway` del order-service se implementa primero con un simulador determinista (aprueba/rechaza según monto o SKU) para poder probar la saga sin pasarela real. En Fase 3.2 se añade `RestPaymentGateway` que llama a `payment-service`.
5. **Estados de la orden.** `CREATED → PAYMENT_PENDING → PAID → SHIPPED → DELIVERED`; compensación: `PAYMENT_FAILED → CANCELLED` con evento `stock.released` (introducido en Fase 4).

## Consecuencias

- (+) Saga verificable sin infraestructura de mensajería.
- (+) Resilience4j demuestra circuit breaker, retry, timeout y bulkhead con métricas.
- (+) Idempotencia evita doble cobro ante reintentos.
- (−) Acoplamiento temporal order↔payment; se desacopla en Fase 4 con eventos.
- (−) El simulador no valida tarjetas reales; suficiente para el portfolio.

## Alternativas consideradas

- **Saga orquestada (orchestrator):** más centralizada pero añade un componente extra; coreografía es suficiente para 2–3 servicios.
- **Kafka desde el inicio:** complejidad prematura; se introduce en Fase 4.
- **Pasarela real (Stripe sandbox):** requiere credenciales y red; el simulador es determinista y testeable.
