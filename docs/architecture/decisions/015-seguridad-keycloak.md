# ADR-015 — Seguridad: Keycloak como IdP, JWT en gateway, deny-by-default

- **Estado:** Aceptado
- **Fecha:** 2026-11-22
- **Decisores:** Architect

## Contexto

El sistema necesita autenticación y autorización para proteger los endpoints. Hasta ahora, todos los endpoints son públicos. Se necesita un IdP, validación de JWT en el gateway, roles (USER/ADMIN) y deny-by-default.

## Decisión

1. **Keycloak 26.8** como IdP (realm `casl`). Se ejecuta en Docker con realm export versionado en el repo (`infra/keycloak/realm-export.json`). El export se importa automáticamente al arrancar.
2. **Gateway como resource server.** El gateway valida el JWT (firma, issuer, audience, expiración) y extrae roles del claim `realm_access.roles`. Los servicios downstream confían en el gateway (zero-trust interno en fases posteriores).
3. **Roles.** `USER` (acceso a endpoints de negocio) y `ADMIN` (gestión de catálogo, usuarios). Deny-by-default: sin token → 401; con token pero rol insuficiente → 403.
4. **Anti-corruption de identidad.** El gateway extrae el `sub` (Keycloak user id) y lo propaga como header `X-User-Id` a los servicios downstream. Los servicios mapean este id a su modelo de usuario propio.
5. **Configuración.** `issuer-uri` y `jwk-set-uri` configurables por variable de entorno. En desarrollo, Keycloak corre en `http://localhost:8180`.
6. **Realm export versionado.** El realm se exporta y versiona en el repo. Cambios en roles/clients requieren actualizar el export y un PR.

## Consecuencias

- (+) Autenticación centralizada y estándar (OIDC/JWT).
- (+) El gateway es el único punto de entrada; los servicios no validan JWT (simplifica, mejora rendimiento).
- (+) Roles y usuarios versionados en el repo.
- (−) El gateway se convierte en punto crítico de seguridad; mitigado por deny-by-default y validación estricta.
- (−) En desarrollo, Keycloak añade un contenedor más; aceptable para el portfolio.

## Alternativas consideradas

- **Spring Authorization Server:** más integrado pero más configuración; Keycloak es más completo y estándar.
- **JWT auto-generado (sin IdP):** no demuestra integración con IdP real.
- **Validación JWT solo en el gateway:** suficiente para el borde, pero no cubre defensa en profundidad.

## Actualización — Fase 5.2 (autorización en servicios downstream)

Se activa validación JWT también en los servicios con operaciones de gestión, sin
depender únicamente del gateway:

| Servicio | Sin token | Rol USER | Rol ADMIN |
| --- | --- | --- | --- |
| `casl-users-service` | 401 | `GET /users`, `GET /users/{id}` | `POST`, `PUT`, `DELETE` |
| `casl-catalog-service` | 401 | lecturas (`products`, `categories`) | escrituras, ajuste/reserva/liberación de stock |
| `casl-gateway` | 401 | enruta `/api/**` | enruta `/api/**` |
| `casl-cart`, `casl-order`, `casl-payment` | 401 en el borde | — | — |

Decisiones derivadas:

1. **Deny-by-default también en cada servicio:** solo `/actuator/health` es público.
   Las reglas finas viven en el controlador con `@PreAuthorize`, cerca del caso de uso.
2. **Errores de seguridad con el contrato RFC 9457:** `401 UNAUTHORIZED` (entry point
   problem+json) y `403 ACCESS_DENIED` (handler dedicado en `ApiExceptionHandler`, que
   antes caía en el catch-all y devolvía 500).
3. **`UserContextFilter` del gateway se ejecuta al final de la cadena de filtros**
   (`Ordered.LOWEST_PRECEDENCE`): con orden previo al `FilterChainProxy` el
   `SecurityContextHolder` todavía está vacío y `X-User-Id` nunca se propagaba.
4. **Pruebas:** los tests de controlador puros excluyen la autoconfiguración de
   seguridad (`@AutoConfigureMockMvc(addFilters = false)`); la autorización se cubre en
   `UserSecurityTest`/`ProductSecurityTest`/`GatewaySecurityTest`. Los tests de
   integración con Testcontainers se autentican con `@WithMockUser`.
5. **Deuda asumida:** la lógica de resource server está duplicada en gateway, users y
   catalog. Se extraerá a un módulo compartido cuando un cuarto servicio la necesite
   (regla de tres), no antes.

## Actualización — Fase 5.3 (anti-corruption de identidad)

1. **La identidad se deriva del token, no de un header.** Los servicios que validan
   el JWT obtienen el sujeto directamente del `SecurityContext` (`@AuthenticationPrincipal
   Jwt`); confiar en `X-User-Id` sería una superficie de spoofing evitable. El header
   sigue existiendo en el gateway para `cart`, `order` y `payment` mientras no validen
   el token por sí mismos, y desaparecerá al completar el zero-trust.
2. **Modelo separado del agregado.** El vínculo vive en `user_identities`
   (`provider`, `subject`) con restricción única y `ON DELETE CASCADE`, en lugar de
   añadir una columna nullable a `users`. La autenticación es otro contexto: el
   agregado `User` no conoce a su proveedor de identidad y se podrá soportar un
   segundo IdP sin migrar el agregado.
3. **Reglas de aprovisionamiento just-in-time** en `GET /api/users/v1/users/me`:
   - Vínculo existente → devuelve el usuario vinculado (idempotente).
   - Sin vínculo y `email_verified=true` → enlaza con el usuario existente de ese email.
   - Sin vínculo y email libre → crea el usuario y lo enlaza.
   - Sin vínculo y email ya registrado sin verificar → `409 USER_EMAIL_ALREADY_EXISTS`
     (un token no verificado no puede apropiarse de un perfil ajeno).
   - Sin email en el token → `400 VALIDATION_ERROR`.
   - Usuario vinculado `INACTIVE` → `403 USER_INACTIVE`.
   - Carrera de aprovisionamiento → la restricción única deja ganar a una petición y la
     otra recibe `409 IDENTITY_ALREADY_LINKED`; reintentar devuelve el vínculo ganador
     (se evita recuperar dentro de la misma transacción, que quedaría marcada para rollback).
4. **Matriz de autorización endurecida:** `GET /users` y `GET /users/{id}` pasan a ser
   solo `ADMIN` (antes cualquier `USER` podía enumerar y leer cualquier perfil). `USER`
   consulta únicamente `GET /users/me`.

## Decisiones abiertas

- Validación JWT en `cart`, `order` y `payment` (zero-trust completo) se planifica para
  Fase 5.4, reutilizando el módulo compartido de seguridad.
- Invalidación de sesiones cuando un usuario pasa a `INACTIVE`: hoy el token sigue
  siendo criptográficamente válido hasta expirar; la comprobación de estado se hace en
  cada llamada a `/me`, y para el resto de endpoints se evaluará en Fase 7.
- Keycloak en modo producción (HTTPS, base de datos externa) se configura en Fase 10 (K8s).

