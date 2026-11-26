# Roadmap CASL

- **Estado:** propuesto, alineado con el estado de `Master` al 2026-11-24
- **Finalidad:** convertir las fases mencionadas en ADRs y PRs en un alcance verificable
- **No es:** un calendario de fechas ni un compromiso de numeración rígida

Las fases 8 y 9 no existían como tal en la documentación previa; se numeran aquí para
dejar explícito el trabajo que hoy está disperso en `docs/security/security-baseline.md`
y en los pendientes de los PRs. La numeración puede reordenarse sin cambiar el alcance.

## Estado actual

| Fase | Entrega | Evidencia en `Master` |
|---|---|---|
| 0 | Monorepo `casl`, convenciones de build, baseline de seguridad | `baf16a7` |
| 1 | API `/api/users/v1/users`, hexagonal, gateway, PII | `b26c690` |
| 2.1–2.3 | Catálogo, carrito en Redis, Trivy en CI y medición de caché | `163ebd3`, `6f2c76e`, `5deec3f` |
| 3 | Order-service, saga síncrona, idempotencia de pago, Resilience4j | `a811429`, `148492b` |
| 4.1–4.3 | Kafka + outbox, notification, shipping, saga coreografiada, inbox/DLT/lag | `c290cd9`, `ca443d3`, `9e8fe22`, `b532a91`, `b5260a3` |
| 5.1–5.4 | Keycloak, JWT, roles, JIT y zero-trust en los ocho desplegables | `ae3827d`, `c422a9a`, `88f6fa5`, `433c84b` |
| 6 | Trazas OTLP, logs JSON correlacionados, métricas y stack local | `3d00e5c` |
| 7 | Estado de usuario en los ocho desplegables, caché y fail-closed | `e6851ff` |
| 8 | Siete specs, colección E2E autenticada y verificación Kafka/DLT | PR #41 |
| Mantenimiento | Wrapper Gradle 9.9.2; Boot 4.2.0 + Spring Cloud 2026.0.0 | `f5eff50`, `f609eb9` |

Resultado: **no quedan PRs abiertos**. Checks requeridos en verde: `unit tests`,
`integration tests`, `CodeQL (java-kotlin)`.

## Principios que no cambian

- Stack 100 % gratuito y reproducible con el wrapper de Gradle y Compose.
- Zero-trust: cada servicio valida el JWT; la identidad sale del `sub`, nunca de `X-User-Id`.
- Arquitectura hexagonal, `problem+json` con `code` estable y gate JaCoCo del 50 % LINE.
- Toda decisión arquitectónica se registra en `docs/architecture/decisions/`.
- Ramas por cambio, Conventional Commits, PR con squash y borrado de rama.

## Fase 7 — Descubrimiento, configuración y estado de identidad

**Estado:** completada (PR #40, `e6851ff`; ADR-017 y ADR-018 aceptados).

Cierra los dos pendientes explícitos de ADR-002 y ADR-015.

**Decisión arquitectónica:** no introducir Eureka ni Spring Cloud Config (ADR-017).
Compose resuelve los servicios por nombre en local y Kubernetes Services + DNS lo hará
en Fase 10. La configuración sigue siendo YAML versionado más variables de entorno.

**Política de degradación (ADR-018):** caché de 60 s con margen de obsolescencia de 300 s;
si `users` no responde se reutiliza el último valor conocido y, si no existe, se deniega con
`503 USER_STATUS_UNAVAILABLE`. No hay fail-open.

Alcance:

1. ADR-017 y cierre del punto “Config Server + Eureka” de ADR-002.
2. `UserStatusEnforcementFilter` en `casl-security` y verificación del estado en **todos**
   los endpoints protegidos, reenviando el JWT del llamador a `users`.
3. `GET /api/users/v1/users/me/status` en `users-v1.yaml` y cableado de la URL
   `USERS_SERVICE_URI` en el perfil `core`.
4. Tests de seguridad para usuarios `INACTIVE` con roles `USER` y `ADMIN` en los
   ocho desplegables.

Criterios de aceptación:

- Un usuario `INACTIVE` recibe `403 USER_INACTIVE` en cualquier endpoint protegido de los
  ocho desplegables, con `USER` o con `ADMIN`.
- Una indisponibilidad de `users` sin caché válida devuelve `503 USER_STATUS_UNAVAILABLE`;
  nunca acceso implícito.
- El `sub` del JWT sigue siendo la única fuente de identidad; no reaparece `X-User-Id`.
- No se añaden contenedores de discovery/config ni secretos nuevos al perfil `core`.
- ADR-002, ADR-015, ADR-018 y este roadmap quedan actualizados.

## Fase 8 — Contratos ejecutables y E2E autenticado

**Estado:** completada (PR #41; ADR-019 aceptada).

Cierra el pendiente “E2E autenticado en Postman” del PR #31 y la ausencia de contratos
para los servicios asíncronos.

Alcance:

1. Especificaciones OpenAPI 3.1 de `shipping-v1` y `notification-v1` en `postman/specs/`.
2. Colección Postman y environment de desarrollo con el flujo Keycloak dev.
3. Flujo E2E de la saga: login → carrito → orden → pago → shipping → notification.
4. Aserciones de idempotencia de pago, entrega de eventos y publicación en DLT.

Criterios de aceptación:

- Los siete specs pasan validación sintáctica y describen los errores `problem+json`.
- La colección se ejecuta contra `docker compose --profile core up -d` sin secretos reales.
- El flujo cubre los Roles `USER` y la cuenta de servicio `casl-order-service`.
- Ningún token ni credencial queda versionado.

**Verificación local:** `python postman/scripts/validate-specs.py` (siete specs OK),
`postman collection lint 'postman/collections/CASL E2E' --fail-severity warning`
(12 requests, sin incidentes) y `postman environment lint 'postman/environments/CASL Dev.environment.yaml'`
(sin incidentes). `postman spec lint` y `postman collection run`
devuelven `401` con el token local caducado; el detalle y la alternativa están en ADR-019.
`postman/README.md` documenta cómo ejecutar la colección y `verify-kafka.ps1`.

## Fase 9 — Hardening de seguridad y calidad

Cierra las filas “diferido” de `docs/security/security-baseline.md`.

**Estado:** completada (PR #42); ADR-020 aceptada.

Alcance:

1. SBOM CycloneDX por módulo (`org.cyclonedx.bom` 3.6.1) y escaneo Trivy de esos SBOM
   en CI, primero *report-only* y después bloqueante para `CRITICAL`/`HIGH` con
   allowlist `.trivyignore.yaml` caducada.
2. DAST con OWASP ZAP **API scan** autenticado contra el stack local y los siete
   contratos OpenAPI. Se descarta el `zap-baseline.py` puro porque el gateway es una API
   sin HTML enlazable y el spider no descubriría rutas (ADR-020).
3. Reevaluar OWASP Dependency-Check frente a Dependabot + SBOM + Trivy. Decisión: no se
   activa; Dependabot cubre el grafo y SBOM + Trivy lo escanean sin coste operativo de
   NVD/H2.
4. Corregir `KEYCLOAK_ISSUER_URI`/`KEYCLOAK_JWK_URI` en los cinco servicios de
   `compose.yaml` que resolvían JWKS contra `localhost` dentro del contenedor y
   devolvían `401` con tokens válidos.
5. Actualizar la tabla de herramientas, la política de severidad y las brechas conocidas.

Criterios de aceptación:

- Los nuevos jobs son reproducibles y su duración está medida antes de hacerlos
  bloqueantes.
- Toda excepción sigue el formato de `docs/security/exceptions.md` con fecha de
  expiración.
- `security-baseline.md` deja de describir controles ya activos como diferidos.

## Fase 10 — Preparación para producción

**Estado:** en curso en `feat/phase-10-prod-prep`; ADR-021 aceptada.

1. Manifiestos Kubernetes y charts Helm para los ocho desplegables y el stack de
   observabilidad; escaneo de IaC con Trivy misconfig/Checkov.
2. Secretos reales mediante un gestor externo; Keycloak con HTTPS y base de datos
   externa; cuenta de servicio `order → payment` sin credenciales de desarrollo.
3. Receptores reales de Alertmanager en lugar del webhook local.
4. Smoke test de arranque completo: Prometheus, Grafana, Tempo, Loki y Promtail con
   trazas, logs y métricas correlacionadas.

Criterios de aceptación:

- Despliegue reproducible en un cluster local (por ejemplo `kind`) sin secretos en Git.
- Las cuatro alertas de `infra/observability/alert-rules.yml` llegan a un receptor real.
- El dashboard `casl-overview` muestra datos de los ocho servicios.

## Decisiones abiertas

- Política exacta de degradación cuando `users` no responde durante la comprobación de
  estado (fail-closed vs fail-open para lectura propia). Se fija antes de cerrar Fase 7.
- Numeración y alcance de Fase 8/9 si aparece trabajo de frontend o de API pública.
