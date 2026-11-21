# ADR-004 — Contrato `/api/users/v1` y errores RFC 9457

- **Estado:** Aceptado
- **Fecha:** 2026-11-21
- **Decisores:** Architect

## Contexto

El servicio arrastraba stubs sin contrato: `GET /rest-api/v1/crud/user/{userId}?id=…`, `POST /rest-api/v1/crud/portfolio`, respuestas de texto plano y modelos internos expuestos. No había especificación, ni códigos de error estables, ni forma de distinguir un cambio intencionado de una regresión. El plan exige **contract-first** y versionado antes del primer cambio incompatible.

## Decisión

### 1. Versionado en la ruta

`/api/{servicio}/v1/{recurso}`. El prefijo de versión vive en la URL, no en headers: es visible, cacheable y trivial de enrutar en el gateway.

Cambios **compatibles** (añadir campo opcional, nuevo endpoint) no crean versión. Cambios **incompatibles** (quitar/renombrar campo, cambiar tipo, cambiar auth) exigen `/v2` y convivencia durante una ventana.

### 2. Recursos, no acciones

| Método | Ruta | Semántica | Éxito |
|---|---|---|---|
| `POST` | `/api/users/v1/users` | Crear | `201` + `Location` |
| `GET` | `/api/users/v1/users/{id}` | Consultar | `200` |
| `GET` | `/api/users/v1/users?page=&size=&status=&email=` | Listar con paginación | `200` |
| `PUT` | `/api/users/v1/users/{id}` | Reemplazar | `200` |
| `DELETE` | `/api/users/v1/users/{id}` | Eliminar | `204` |

### 3. Errores: RFC 9457 (`application/problem+json`)

Cuerpo único para todos los errores, con extensión `code` estable y `errors[]` para validación:

```json
{
  "type": "https://errors.casl.dev/user-not-found",
  "title": "User not found",
  "status": 404,
  "detail": "No user with id 3f1c…",
  "instance": "/api/users/v1/users/3f1c…",
  "code": "USER_NOT_FOUND"
}
```

`code` es el contrato legible por máquina; `detail` es para humanos y puede cambiar. Nunca se devuelve stacktrace ni el mensaje de una excepción interna.

### 4. Modelo de datos de la API

- `id`: UUID generado por el servidor.
- `email`: normalizado a minúsculas, único (índice único). Duplicado → `409 USER_EMAIL_ALREADY_EXISTS`.
- `firstName`, `lastName`: obligatorios, 1–100 caracteres.
- `phone`: opcional, hasta 30 caracteres.
- `status`: `ACTIVE` | `INACTIVE`.
- `createdAt`, `updatedAt`: servidor, ISO-8601 UTC.
- El recurso de salida **no** expone entidades de persistencia ni campos internos.

### 5. Paginación

`page` (0-based, default 0) y `size` (default 20, máximo 100). Respuesta con `content`, `page`, `size`, `totalElements`, `totalPages`. Se documenta que page/offset puede saltar elementos si los datos cambian; el cursor queda para cuando exista un feed de alto volumen.

### 6. Idempotencia

`GET`, `PUT` y `DELETE` son idempotentes por diseño. `POST` no lo es: la unicidad de email evita duplicados accidentales (`409`), y una `Idempotency-Key` se incorporará cuando exista un endpoint con efectos externos (pagos, Fase 3).

### 7. Artefactos

- `postman/specs/users-v1.yaml`: fuente de verdad OpenAPI 3.1.
- Colección Postman con assertions: se versiona en `postman/collections/`.
- Un cambio de comportamiento sin actualizar el contrato es un defecto de PR.

## Consecuencias

- (+) Contrato explícito, testeable y consumible por tooling y por el gateway.
- (+) Errores uniformes: el cliente programa contra `code`, no contra textos.
- (+) La ruta legacy desaparece sin deuda (no tenía consumidores).
- (−) `PUT` exige enviar el recurso completo; los clientes que quieran actualizaciones parciales necesitarán `PATCH` (evaluado en Fase 2).
- (−) Mantener spec + colección + tests añade trabajo por endpoint; se acepta por la trazabilidad que aporta.
