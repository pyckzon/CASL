# ADR-018 — Comprobación del estado del usuario en todos los endpoints

- **Estado:** Aceptado
- **Fecha:** 2026-11-24
- **Decisores:** Architect

## Contexto

Fase 5 dejó la validación de identidad (JWT) en los ocho desplegables, pero la comprobación
del estado del usuario (`User.status`) solo ocurría en `GET /api/users/v1/users/me`, donde
además se hace el aprovisionamiento JIT. Un usuario dado de baja con rol `ADMIN` podía
seguir escribiendo en catálogo, carrito, orden o pago hasta que expirara su token: el JWT
sigue siendo criptográficamente válido porque Keycloak no revoza tokens por el estado de
la fila de `users`.

El roadmap (Fase 7) pidió extender la comprobación a todos los endpoints con una llamada
acotada a `users`, caché corta y política de degradación documentada. ADR-017 descartó
añadir descubrimiento o configuración remota para resolver la URL de `users`.

## Decisión

1. **`users` es la única fuente de verdad** del estado. No se replica en Keycloak ni en
   otros servicios.
2. **Punto de aplicación único:** `UserStatusEnforcementFilter` en `casl-security`, añadido
   a la cadena `@Order(2)` después de la autenticación Bearer. Solo inspecciona `/api/**`
   del puerto de aplicación; la cadena `@Order(1)` del puerto management no lo incluye.
3. **Contrato interno del módulo compartido:** `UserStatusVerifier.verify(Jwt)` devuelve
   `ACTIVE` o `INACTIVE` y lanza `UserStatusUnavailableException` si no puede verificar.
   La identidad sigue saliendo del `sub` validado; no se añade ningún header propagado.
4. **Dos implementaciones:**
   - En `users`: `LocalUserStatusVerifier` consulta `UserUseCase.statusOf(profile)` (que
     reutiliza el JIT). Sin caché: la baja de un usuario es inmediata en el servicio que
     posee el dato.
   - En los otros siete desplegables: `RemoteUserStatusVerifier` reenvía el **mismo JWT
     del llamador** a `GET /api/users/v1/users/me/status`. No se crea ni se distribuye un
     secreto de cuenta de servicio nuevo. Caché en memoria por `sub` con TTL de 60 s y
     margen adicional de 300 s (ventana total máxima de 360 s).
5. **Degradación:** si `users` no responde (timeout, conexión rechazada o 5xx), se reutiliza
   el último valor en caché mientras la antigüedad no supere 360 s. Si no hay valor en caché, se
   deniega con `503 USER_STATUS_UNAVAILABLE`. **No existe fail-open**: un usuario cuyo
   estado no se ha podido verificar no obtiene acceso. El 503 es reintentable y distinto
   de un 403 de autorización.
6. **Códigos de error uniformes:** usuario inactivo → `403 USER_INACTIVE` (el mismo código
   que ya usaba `/me`, sin romper `users-v1.yaml`); indisponibilidad → `503
   USER_STATUS_UNAVAILABLE`. Ambos en `application/problem+json`.
7. **Exclusiones explícitas:** tokens de servicio (`ServiceToken.isService`, cubre
   `order → payment` con `client_credentials`; por diseño, cualquier service account del
   realm se excluye porque representa una integración, no una persona) y, dentro de `users`,
   `/me` y `/me/status`,
   que ya comprueban el estado por su propia vía y evitarían una recursión HTTP.
8. **Configuración:** `casl.security.user-status.base-url` con variable de entorno
   `USERS_SERVICE_URI` (por defecto `http://localhost:8666`). En Compose se resuelve por
   nombre de servicio; sin Eureka ni Config Server (ADR-017).
9. **Contrato:** nuevo `GET /api/users/v1/users/me/status` en `postman/specs/users-v1.yaml`,
   devuelve `{"status":"ACTIVE"|"INACTIVE"}` sin PII y sin 403 para usuarios inactivos.

## Alternativas rechazadas

- **Claim personalizado en Keycloak:** exigiría sincronizar el alta/baja desde `users` al
  IdP mediante su API de administración; los tokens ya emitidos seguirían desactualizados
  y acoplaría dos dominios.
- **Cuenta de servicio compartida para consultar `users`:** distribuiría un secreto a siete
  servicios y convertiría una consulta de estado en una credencial amplia.
- **Fail-open cuando `users` caiga:** mantendría la disponibilidad, pero permitiría operar
  a usuarios dados de baja durante el incidente; incompatible con la postura zero-trust.
- **Comprobar solo en el gateway:** los servicios volverían a confiar en un header o en que
  todas las peticiones pasan por el gateway, rompiendo la validación por servicio.
- **Sin caché:** una llamada HTTP por petición y por servicio; latencia innecesaria para un
  dato que cambia pocas veces.

## Consecuencias

- (+) Un usuario `INACTIVE` con rol `USER` o `ADMIN` no puede operar en ningún desplegable.
- (+) No hay secretos nuevos ni contenedores nuevos; la URL es configuración versionada.
- (−) Hasta 60 s de desfase en la revocation fuera de `users`; se acepta y se documenta.
- (−) Si `users` cae y no hay caché, toda la API devuelve 503: se prefiere indisponibilidad
  explícita a acceso no verificado.
- (−) Un http call por `sub`, servicio y minuto; acotado y con timeouts de 1 s / 2 s.

## Verificación

- Tests del filtro en `casl-security`: ACTIVE pasa, INACTIVE → 403 `USER_INACTIVE`, sin
  token → 401, token de servicio omitido, caché reutilizada, degradation con y sin caché.
- Test de `users`: `statusOf` hace JIT y devuelve el estado real; `/me/status` no expone PII.
- Un test de seguridad por desplegable con `INACTIVE` y roles `USER`/`ADMIN`.
- `./gradlew test` y `./gradlew integrationTest` en verde con el gate JaCoCo del 50 % LINE.
