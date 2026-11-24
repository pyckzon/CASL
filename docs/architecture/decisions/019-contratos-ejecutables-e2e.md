# ADR-019 — Contratos ejecutables y E2E autenticado sin credenciales versionadas

- **Estado:** Aceptado
- **Fecha:** 2026-11-24
- **Decisores:** Architect

## Contexto

La Fase 8 del roadmap cerraba dos pendientes: el “E2E autenticado en Postman” del PR #31
y la ausencia de contratos para los servicios asíncronos (`shipping` y `notification`).
Antes de esta decisión existían cinco specs (`users`, `catalog`, `cart`, `orders`,
`payments`), los directorios `postman/{collections,environments,flows,...}` vacíos y
cuatro `$ref` relativos apuntando a `../users-v1.yaml`, que en realidad resuelve a
`postman/users-v1.yaml` (archivo inexistente): los specs no eran totalmente válidos.

Además, la CLI de Postman 1.74.0 responde `401 Access token validation failed` a
`spec lint` y a `collection run` en este entorno, aunque `whoami` informe una sesión
autenticada. `flows run` está documentado como función de planes Enterprise y con
workspace para bloques conector. Ninguna de esas limitaciones es una caída de cuota,
así que no aplica cambiar de plan ni reintentar.

## Decisión

1. **Siete specs OpenAPI 3.1.** Se añaden `shipping-v1.yaml` (tres operaciones HTTP reales)
   y `notification-v1.yaml`. Este último usa `paths: {}` porque el servicio no expone
   HTTP de negocio: se documentan los cuatro topics consumidos, el sobre
   `DomainEventEnvelope`, la idempotencia por `inbox` y el DLT `<topic>.DLT`. No se
   inventan rutas para hacerlo “parecer” un API sincrónico. Se rechaza AsyncAPI porque
   añadiría una segunda herramienta de validación sin consumidores en el repositorio.
2. **Un único esquema `Problem`.** Los siete specs reutilizan
   `./users-v1.yaml#/components/schemas/Problem` y describen `application/problem+json`,
   incluidos `USER_INACTIVE` y `USER_STATUS_UNAVAILABLE` de la Fase 7. Se corrigen los
   `$ref` relativos rotos de `cart`, `catalog`, `orders` y `payments`.
3. **Validación sintáctica local reproducible.** `postman/scripts/validate-specs.py`
   comprueba YAML, versión 3.1, presencia de `paths`, esquema `Problem` y resolución
   de referencias locales y relativas. Sustituye a `postman spec lint` mientras el token
   local esté caducado; no sustituye a las reglas de gobierno de la CLI.
4. **Colección v3 Git-native, no un Flow JSON.** `collections/CASL E2E/` contiene
   doce requests YAML con aserciones `pm.test` encadenadas por variables de colección.
   Un Flow local no se puede ejecutar sin plan Enterprise ni validar aquí; la colección
   sí pasa `postman collection lint` en local y cubre el mismo recorrido.
5. **Environment sin secretos.** `environments/CASL Dev.environment.yaml` deja vacíos
   y marcados como `secret` el password USER, el password ADMIN y el client secret de
   `casl-order-service`. La copia `*.local.environment.yaml` está ignorada por Git.
   Ningún token ni credencial se versiona; el flujo usa los usuarios dev públicos del
   realm `casl` y el token `client_credentials` que se emite en tiempo de ejecución.
6. **Verificación Kafka fuera de Postman.** Ni una colección ni un Flow pueden leer
   tópicos. `postman/scripts/verify-kafka.ps1` usa los binarios de Kafka y `psql` del
   perfil `core` para aseverar entrega de `order.created.v1`/`payment.approved.v1`,
   idempotencia de `notification` (mismo número de filas en `notifications` y una sola
   fila en `inbox` al repetir un `eventId`) y publicación en `payment.approved.v1.DLT`
   tras agotar reintentos. El contador `casl.dlt.published` queda como señal complementaria.
7. **Corrección encontrada por el E2E:** `NotificationService` persiste una fila por
   canal (email, log, webhook) con el mismo `event_id`, pero V1 declaraba
   `UNIQUE(event_id)`. El segundo `saveAndFlush` violaba la restricción, impedía marcar
   el `inbox` y enviaba cada evento al DLT. La migración V2 sustituye esa restricción
   por `UNIQUE(event_id, channel)`. Sin este cambio la aserción de entrega de eventos
   no podría cumplirse.

## Alternativas rechazadas

- **Publicar un Flow en el workspace:** cambia estado compartido, exige consentimiento
  y no aporta ejecutabilidad local con el plan actual.
- **Añadir AsyncAPI y su toolchain:** duplicaría la fuente de verdad del contrato sin un
  consumidor automatizado en el repositorio.
- **Versionar las credenciales dev:** incumple el criterio de aceptación y normaliza
  secretos en Git aunque sean de desarrollo.
- **Dar por buena la validación de la CLI:** `spec lint` y `collection run` fallan con
  401 por un token local caducado; depender de ello haría la comprobación no reproducible.

## Consecuencias

- (+) Los siete specs pasan validación sintáctica y referencial sin servicios remotos.
- (+) La colección es revisable request a request en el PR y ejecutable con un `postman login` vigente.
- (+) Las garantías asíncronas se comprueban contra el stack real, no contra un mock.
- (−) La ejecución de la colección sigue requiriendo un token de Postman válido; en este
  entorno solo se pudo verificar `collection lint` y `environment lint`.
- (−) `verify-kafka.ps1` asume el perfil `core` levantado con los nombres de servicio de
  `compose.yaml`; no es portable a un cluster sin adaptarlo.

## Verificación

- `python postman/scripts/validate-specs.py` → siete specs OK, 0 errores.
- `postman collection lint 'postman/collections/CASL E2E' --fail-severity warning`
  → 12 requests, 0 errores, 0 advertencias.
- `postman environment lint 'postman/environments/CASL Dev.environment.yaml'`
  → 0 errores, 0 advertencias.
- `postman spec lint` y `postman collection run` → 401; se documenta como limitación
  del entorno, no como resultado de la validación.
