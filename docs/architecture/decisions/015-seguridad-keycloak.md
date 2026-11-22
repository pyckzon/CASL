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
- **Validación JWT en cada servicio:** redundante; el gateway es suficiente para este tamaño.

## Decisiones abiertas

- Validación JWT en cada servicio (zero-trust) se evalúa en Fase 7 (integraciones enterprise).
- Keycloak en modo producción (HTTPS, base de datos externa) se configura en Fase 10 (K8s).
