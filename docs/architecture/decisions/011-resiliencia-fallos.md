# ADR-011 — Resiliencia: Resilience4j con pruebas de inyección de fallos

- **Estado:** Aceptado
- **Fecha:** 2026-11-22
- **Decisores:** Architect

## Contexto

La saga síncrona order→payment (Fase 3) depende de un servicio remoto. Sin resiliencia, un fallo o latencia del servicio de pago se propaga como 500 y deja la orden en estado inconsistente. Se necesita circuit breaker, retry, timeout y bulkhead, con verificación empírica.

## Decisión

1. **Resilience4j 2.4.0** protege la llamada a `PaymentGateway.charge()` en `OrderApplicationService` con `@CircuitBreaker`, `@Retry`, `@Bulkhead` y `@TimeLimiter` (fallback a `PaymentResult.rejected`).
2. **Configuración por instancia `payment`**: sliding window 10, failure rate 50%, wait 10s en open, retry 3 con backoff exponencial (500ms ×2), timeout 3s, bulkhead 10 llamadas concurrentes.
3. **Verificación empírica** mediante `ResilienceIntegrationTest`:
   - Fallo transitorio → retry 3 veces → fallback → orden CANCELLED.
   - Latencia > timeout → TimeLimiter cancela → fallback.
   - 5 fallos consecutivos → circuito OPEN → siguiente llamada no invoca el gateway.
   - Métricas expuestas en `/actuator/metrics` (prefijo `resilience4j.*`).
4. **Métricas** habilitadas vía `resilience4j-micrometer` y expuestas en Prometheus.

## Consecuencias

- (+) La saga degrada controladamente: el cliente recibe 200 con estado CANCELLED, no 500.
- (+) El circuit breaker evita cascada de fallos cuando payment-service cae.
- (+) Las métricas permiten observar el estado del circuito, reintentos y latencia.
- (−) El timeout de 3s + 3 reintentos puede tardar ~12s en el peor caso; aceptable para el portfolio.
- (−) Los tests de resiliencia requieren Postgres (Testcontainers); se validan en CI.

## Alternativas consideradas

- **Hystrix:** deprecado; Resilience4j es el estándar actual para Spring Boot 4.
- **Retry manual con try/catch:** pierde las métricas y la observabilidad del circuito.
- **Sin timeout (confiar en el cliente HTTP):** no protege contra latencia infinita.
