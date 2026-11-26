# External Secrets — Fase 10 (producción)

## Versiones fijadas
- External Secrets Operator: **2.15.0** (chart `external-secrets-2.15.0`)
- Vault Helm chart: **0.34.1** (Vault engine 2.0.4 por defecto)

## Secretos gestionados (nombres de rutas en Vault KV v2, prefijo `secret/casl/`)

| Secret K8s | Ruta Vault | Propiedad | Consumidor |
|---|---|---|---|
| `casl-order-client` | `secret/casl/order` | `client_secret` | `casl-order-service` (`KEYCLOAK_CLIENT_SECRET`) |
| `casl-postgres` | `secret/casl/postgres` | `password` | Postgres + servicios (`SPRING_DATASOURCE_PASSWORD`) |
| `casl-keycloak` | `secret/casl/keycloak` | `db_password`, `order_client_secret` | Keycloak (`KC_DB_PASSWORD`, `KC_CASL_ORDER_SECRET`) |

## Inicialización manual de Vault (dev, sin secretos en Git)

1. Instalar ESO (CRDs gestionados por el chart, `server-side apply` por tamaño >256 KB):

   ```bash
   helm repo add external-secrets https://charts.external-secrets.io
   helm upgrade --install external-secrets external-secrets/external-secrets \
     -n external-secrets --create-namespace --version 2.15.0 --wait
   ```

2. Instalar Vault en modo dev (solo local):

   ```bash
   helm repo add hashicorp https://helm.releases.hashicorp.com
   helm upgrade --install vault hashicorp/vault -n vault --create-namespace \
     --version 0.34.1 --set server.dev.enabled=true --wait
   ```

3. Generar el root token fuera de Git y materializarlo como Secret bootstrap:

   ```bash
   export VAULT_TOKEN="$(openssl rand -hex 32)"
   kubectl -n casl create secret generic vault-token --from-literal=token="$VAULT_TOKEN"
   ```

4. Escribir los secretos en Vault (nunca en Git):

   ```bash
   kubectl -n vault exec -it vault-0 -- sh -c "export VAULT_TOKEN=$VAULT_TOKEN; \
     vault kv put secret/casl/order client_secret=<valor>; \
     vault kv put secret/casl/postgres password=<valor>; \
     vault kv put secret/casl/keycloak db_password=<valor> order_client_secret=<valor>"
   ```

5. Aplicar `SecretStore` + `ExternalSecret`:

   ```bash
   kubectl apply -f infra/k8s/external-secrets/
   kubectl -n casl get externalsecret  # debe mostrar SecretSynced
   ```

6. Verificar materialización: `kubectl -n casl get secret casl-order-client -o jsonpath='{.data.KEYCLOAK_CLIENT_SECRET}' | base64 -d`.

## Flujo de trabajo

- Vault dev es en memoria y efímero: aceptable en local, **no** en producción.
- En producción: Vault con persistencia + unseal (o KMS), auth method Kubernetes (`audiences: [vault]`, requerido desde Vault 1.21), y `refreshInterval` de 15 s.
- Rotación: cambiar el valor en Vault; ESO refresca el Secret; reiniciar `casl-order-service` si el token cacheado no se invalida (cache de 30 s en `KeycloakServiceTokenProvider`).
- `realm-export.json` usa placeholders `${KC_CASL_ORDER_SECRET}` (ADR-021 §4); el valor real lo materializa ESO hacia Keycloak.

## Importante

- **Nunca commit de secretos en Git**: solo referencias (`SecretStore`/`ExternalSecret`).
- El root token se inicializa manualmente desde el shell y no se almacena en el repositorio.
- `rootToken` vacío en `SecretStore` se resuelve por `tokenSecretRef` al Secret `vault-token`.