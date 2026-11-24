# Contratos ejecutables y E2E autenticado (Fase 8)

## Specs

Los siete contratos OpenAPI 3.1 viven en `specs/`. `shipping-v1.yaml` describe la
API HTTP real; `notification-v1.yaml` tiene `paths: {}` porque el servicio solo
consume Kafka (no se inventan endpoints). Todos reutilizan el esquema `Problem`
de `users-v1.yaml` y documentan los errores de la Fase 7
(`USER_INACTIVE`, `USER_STATUS_UNAVAILABLE`).

`postman spec lint` de la CLI 1.74.0 devuelve `401 Access token validation failed`
en este entorno aunque `postman whoami` informe sesión autenticada. Para no
depender de un token remoto, la validación sintáctica local es:

```powershell
python postman/scripts/validate-specs.py
```

Comprueba YAML, versión 3.1, `paths`, esquema `Problem` y resolución de todos los
`$ref` (incluidos los relativos entre specs). Requiere PyYAML.

## Colección y environment

- `collections/CASL E2E/` es una colección v3 Git-native: cada request es un YAML
  independiente y las aserciones `pm.test` viven junto a la petición.
- `environments/CASL Dev.environment.yaml` **no contiene credenciales**: los
  valores de usuario/contraseña/client secret están vacíos y marcados como secretos.

Para ejecutarla:

1. `docker compose --profile core up -d --wait`
2. `postman login` (necesario para `postman collection run`)
3. Copiar el environment a `environments/CASL Dev.local.environment.yaml` (ignorado
   por Git) y rellenar `dev_username`, `dev_password`, `admin_username`,
   `admin_password` y `order_client_secret` con los valores **dev** del realm `casl`.
4. `postman collection run 'postman/collections/CASL E2E' -e 'postman/environments/CASL Dev.local.environment.yaml' --report-events=false -r junit,html`
5. `./postman/scripts/verify-kafka.ps1`

La colección encadena: login USER/ADMIN/cuenta de servicio → producto → carrito →
orden → `pay` (USER → order → payment con client_credentials) → dos cobros con la
misma `Idempotency-Key` (mismo `paymentId`) → shipment creado por
`payment.approved.v1` → entrega `DELIVERED`.

Paso 5 comprueba lo que Postman no puede observar: entrega de
`order.created.v1` y `payment.approved.v1`, idempotencia de notification por
`eventId` (tablas `notifications` e `inbox`) y publicación en
`payment.approved.v1.DLT` tras agotar los reintentos.

## Por qué no hay un archivo de Postman Flow

`postman flows run` está restringido a planes Enterprise y requiere login, además
de un workspace para los bloques conector. La colección v3 es ejecutable en local
con la CLI y mantiene el mismo flujo y aserciones; la verificación Kafka se hace
con el script porque ni la colección ni los Flows leen tópicos directamente. Esta
decisión se registra en el ADR-019.
