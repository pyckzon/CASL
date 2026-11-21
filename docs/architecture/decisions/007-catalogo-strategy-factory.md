# ADR-007 — Catálogo: Strategy para búsqueda, Factory para mapeo, Adapter para caché

- **Estado:** Aceptado
- **Fecha:** 2026-11-21
- **Decisores:** Architect

## Contexto

El catálogo necesita buscar productos por múltiples criterios combinables (nombre, categoría, rango de precio, estado) y devolver resultados paginados. Los criterios crecerán (búsqueda por texto, por atributos, por disponibilidad). Además, el mapeo entre DTOs HTTP, entidades JPA y el agregado de dominio se repetirá en varios adaptadores, y el acceso a caché (Redis) debe poder desactivarse o cambiarse de proveedor sin tocar el dominio.

## Decisión

1. **Strategy para búsqueda.** Se define el puerto `ProductSearchStrategy` con un único método `search(ProductSearchCriteria, page, size)`. La implementación principal (`JpaProductSearchStrategy`) traduce los criterios a una `Specification` de Spring Data. Añadir una nueva forma de búsqueda (full-text, por atributos) es añadir una implementación, no modificar el dominio ni el caso de uso. La fake en memoria de los tests es la segunda implementación real del puerto.
2. **Factory para mapeo.** `ProductFactory` concentra la creación del agregado a partir de comandos y la traducción a DTOs de respuesta. `ProductPersistenceMapper` y `ProductWebMapper` delegan en él para no duplicar reglas (normalización de SKU, Money, status por defecto).
3. **Adapter para caché.** Redis se accede únicamente a través de `CacheManager`/`@Cacheable` en el adaptador de aplicación; el dominio no conoce caché. Un `CachedProductSearchStrategy` (decorator) envuelve a la estrategia JPA y se activa por configuración (`casl.catalog.cache.enabled`). Desactivar la caché no cambia el caso de uso.
4. **StockPolicy como puerto.** La reserva de stock se delega a `StockPolicy` (implementación inicial `OptimisticStockPolicy`). El dominio expone `reserve`/`release` sobre `Product`; la política decide si la reserva es posible. Permite añadir políticas pesimistas o backorder sin tocar el agregado (OCP, §2.3).
5. **Money y Sku como value objects.** `Money` (cantidad + moneda) y `Sku` (código normalizado) son records con validación; se persisten descompuestos en el adaptador.

## Consecuencias

- (+) Nuevos criterios de búsqueda, proveedores de caché o políticas de stock se añaden sin modificar dominio ni casos de uso.
- (+) El dominio sigue puro (ArchUnit lo verifica).
- (+) La caché es medible: hit/miss por región vía Micrometer (`cache.gets`).
- (−) Más clases que un CRUD directo; aceptado por la trazabilidad del portfolio.
- Riesgo: sobre-abstracción si se añaden estrategias sin consumidor real; mitigado por la regla de crear el puerto solo con segunda implementación real.

## Alternativas consideradas

- **Búsqueda con parámetros opcionales en un único método del repositorio:** más simple, pero los criterios no son composables y la lógica de filtrado se acumula en el repositorio.
- **Caché con `@Cacheable` directo en el caso de uso:** acopla el caso de uso a Spring Cache; preferimos el decorator para que el dominio permanezca framework-free.
- **Stock como entidad separada:** considerado, pero la reserva es una operación del agregado `Product` en este tamaño; se mantiene en el mismo agregado con `StockPolicy` desacoplado.
