# ADR-008 — Carrito: Redis como almacén, anti-corruption layer hacia catálogo

- **Estado:** Aceptado
- **Fecha:** 2026-11-21
- **Decisores:** Architect

## Contexto

El carrito es un agregado de corta vida, de alta frecuencia de lectura/escritura y sin necesidad de durabilidad fuerte: se pierde si el usuario abandona la sesión. Persistirlo en PostgreSQL añadiría latencia y migraciones sin beneficio. Además, el carrito necesita datos del catálogo (precio, nombre, disponibilidad) que no son suyos: tomarlos directamente acoplaría el carrito al modelo de catálogo.

## Decisión

1. **Almacén en Redis.** El carrito se serializa como JSON en Redis con TTL configurable (`casl.cart.ttl`). No hay tabla en base de datos. Si Redis no está disponible, el carrito se pierde (aceptable para este dominio); la degradación se documenta.
2. **Anti-corruption layer.** El carrito no conoce `Product` del catálogo. Define su propio `CartItem` (sku, nombre, precio, cantidad) y obtiene los datos vigentes a través del puerto `CatalogClient`, cuya implementación (`RestCatalogClient`) traduce el modelo del catálogo al del carrito. Cambiar el catálogo no toca el dominio del carrito.
3. **Validación al añadir.** Al añadir o actualizar un item, el caso de uso consulta al catálogo: si el producto no existe o no tiene stock suficiente, se rechaza con `PRODUCT_NOT_AVAILABLE`. El precio se toma del catálogo (fuente de verdad), no del cliente.
4. **Idempotencia de operaciones.** Añadir el mismo SKU incrementa la cantidad; no crea duplicados. Eliminar un item inexistente es no-op (idempotente).
5. **Merge de carritos.** Al autenticar un usuario con carrito anónimo, los items se fusionan en el carrito del usuario (cantidades sumadas).
6. **Checkout preparatorio.** El carrito expone un snapshot inmutable (`CartSnapshot`) con los items y el total; la creación de la orden (Fase 3) consume ese snapshot.

## Consecuencias

- (+) Latencia baja y sin migraciones para un agregado efímero.
- (+) El carrito sobrevive a reinicios del servicio (estado en Redis).
- (+) El dominio del carrito es independiente del catálogo (anti-corruption).
- (−) El carrito se pierde si Redis se vacía; aceptable y documentado.
- (−) Requiere Redis disponible; el gateway y el servicio degradan si no lo está.
- Riesgo: precios desactualizados si el catálogo cambia entre añadir y checkout; mitigado tomando el precio del catálogo al añadir y revalidando en checkout (Fase 3).

## Alternativas consideradas

- **PostgreSQL para el carrito:** durabilidad innecesaria y migraciones para un agregado efímero.
- **Carrito en el navegador (stateless):** no permite merge entre dispositivos ni recuperación tras cierre.
- **Compartir entidades de catálogo en el carrito:** acoplamiento directo; descartado por anti-corruption.
