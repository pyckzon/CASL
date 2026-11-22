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
| `casl-cart`, `casl-order`, `casl-payment` | 401 | lectura/escritura del propio recurso | lectura/escritura (payment: cobro/devolución) |

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
5. **Deuda asumida (resuelta en 5.4):** la lógica de resource server estaba duplicada en
   gateway, users y catalog; se extrae al módulo `casl-security` en la Fase 5.4.

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

## Actualización — Fase 5.4 (zero-trust completo)

Deja sin efecto el punto 4 de la decisión original y el punto 3 de la Fase 5.2: el
gateway ya no propaga `X-User-Id` y el `UserContextFilter` se elimina. Cada servicio
valida el JWT por su cuenta.

1. **Módulo `casl-security`.** Librería Gradle con `CaslSecurityConfiguration`
   (deny-by-default, health público, `/api/**` autenticado), convertidor de roles de
   `realm_access.roles` y entry point/handler que devuelven `401 UNAUTHORIZED` y
   `403 ACCESS_DENIED` en `application/problem+json`. Se importa explícitamente con
   `@Import(CaslSecurityConfiguration.class)` en cada `SecurityConfig`: sin
   auto-configuración mágica, el cableado queda visible en cada servicio.
2. **Identidad en `cart` y `order`.** El dueño se obtiene del `sub` con
   `JwtSubject.requireUuid` y se comprueba con `JwtSubject.requireOwner` antes de
   cualquier operación. Desaparece `userId` del query de creación de carrito y del body
   de creación de pedido; un cliente ya no puede elegir al dueño (IDOR cerrado).
3. **Llamada `order` → `payment`.** En lugar de reenviar el token del usuario, `order`
   usa una cuenta de servicio de Keycloak (`casl-order-service`, `client_credentials`)
   con token cacheado hasta 30 s antes de expirar. `payment` acepta esa cuenta
   (`preferred_username=service-account-casl-order-service` y
   `client_id=casl-order-service`) o un `ADMIN` mediante `ServiceToken.requireManager`.
   El `sub` de la cuenta de servicio es un UUID, igual que el de un usuario, por lo que
   no sirve para distinguirlos. Así el usuario no necesita permisos de cobro y se mantiene
   el principio de menor privilegio.
4. **Contratos y pruebas.** Las specs OpenAPI de cart/order/payment declaran bearer
   JWT, 401/403 y la ausencia de `userId` de entrada. Los tests de seguridad cubren
   token ausente, rol insuficiente, acceso ajeno y cuenta de servicio; los integration
   tests se autentican con `jwt()` del subject correcto.
5. **CSRF no aplica.** La autenticación es stateless: el JWT viaja en `Authorization:
   Bearer`, nunca en cookies, y el navegador no lo adjunta automáticamente a peticiones
   cross-site. La cadena compartida configura `csrf.ignoringRequestMatchers(anyRequest)`
   en lugar de `csrf.disable()`: expresa que ninguna petición necesita protección CSRF
   sin desactivar el mecanismo. Si en el futuro se introducen cookies de sesión, hay que
   revertir esta decisión.

**Consecuencias:** (+) defensa en profundidad sin depender de la red interna; (+) no
hay suplantación por header ni por body; (−) validación JWT en cada salto; (−) el
secreto de la cuenta de servicio es de desarrollo y debe migrarse a un gestor de
secretos en Fase 10.

## Decisiones abiertas

- Invalidación de sesiones cuando un usuario pasa a `INACTIVE`: hoy el token sigue
  siendo criptográficamente válido hasta expirar; la comprobación de estado se hace en
  cada llamada a `/me`, y para el resto de endpoints se evaluará en Fase 7.
- Keycloak en modo producción (HTTPS, base de datos externa) se configura en Fase 10 (K8s).

