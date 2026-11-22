# ADR-010 — Payment service: idempotencia por clave y pasarela simulada

- **Estado:** Aceptado
- **Fecha:** 2026-11-22
- **Decisores:** Architect

## Contexto

La saga de compra (Fase 3) requiere que el cobro sea idempotente: reintentar el pago de la misma orden no debe cobrar dos veces. Además, el order-service necesita llamar a un servicio de pago real (no un simulador embebido) para demostrar comunicación síncrona entre microservicios, anti-corruption layer y Resilience4j sobre una dependencia remota.

## Decisión

1. **`casl-payment-service`** como microservicio independiente (base de datos propia, puerto 8673). Expone `POST /api/payments/v1/payments` con `Idempotency-Key` en el header.
2. **Idempotencia por clave.** Tabla `payments` con restricción única sobre `idempotency_key`. Si llega una petición con una clave ya procesada, se devuelve el resultado almacenado sin volver a cobrar. La clave es el `orderId` (UUID), lo que garantiza idempotencia por orden.
3. **Pasarela simulada (Adapter).** El dominio del payment-service define el puerto `PaymentProcessor` (simula aprobación/rechazo por monto, igual que el simulador del order-service). Sustituible por un adaptador real (Stripe, PayPal, MercadoPago) sin tocar el dominio.
4. **`RestPaymentGateway` en order-service.** Reemplaza al `SimulatedPaymentGateway`. Llama a payment-service vía REST con la `Idempotency-Key`. Si payment-service no está disponible, Resilience4j aplica circuit breaker/retry/timeout/bulkhead y el fallback marca la orden como `PAYMENT_FAILED`.
5. **Reembolso.** `POST /api/payments/v1/payments/{id}/refund` marca el pago como `REFUNDED` (simulado).
6. **Anti-corruption.** El payment-service no conoce el modelo de orden ni de catálogo; solo recibe monto, moneda e idempotency key.

## Consecuencias

- (+) Cobro idempotente: reintentos no generan doble cargo.
- (+) Comunicación síncrona real entre microservicios con Resilience4j.
- (+) Pasarela intercambiable (OCP).
- (−) El simulador no valida tarjetas reales; suficiente para el portfolio.
- (−) Dos servicios adicionales (order + payment) deben estar up para el flujo completo.

## Alternativas consideradas

- **Mantener el simulador embebido en order-service:** no demuestra comunicación inter-servicio ni anti-corruption.
- **Pasarela real (Stripe sandbox):** requiere credenciales y red externa; el simulador es determinista y testeable.
- **Idempotencia en el cliente (no repetir):** no es suficiente; el servidor debe ser idempotente.
