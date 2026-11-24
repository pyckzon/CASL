# ADR-017 — Descubrimiento y configuración sin Eureka ni Config Server

- **Estado:** Aceptado
- **Fecha:** 2026-11-24
- **Decisores:** Architect

## Contexto

ADR-002 dejó pendiente decidir en Fase 7 entre introducir **Config Server + Eureka** o
mantener un enfoque más simple, “según el peso del portfolio”. Hoy el sistema tiene ocho
desplegables, rutas de gateway estáticas, validación JWT en cada servicio (zero-trust) y
perfiles de Compose que arrancan el entorno completo sin registro externo. La Fase 10
prevé Kubernetes, que ya ofrece descubrimiento y configuración nativos.

Añadir Eureka y Config Server implicaría dos servicios más que arrancar, una dependencia
de red durante el arranque y nueva superficie de ataque, sin resolver ningún problema
abierto de autorización o de observabilidad.

## Decisión

1. **No introducir Eureka ni Spring Cloud Config Server.**
2. **Descubrimiento:** nombres de servicio de Compose en local; Kubernetes Services y
   DNS interna en Fase 10. Las URLs de clientes HTTP siguen siendo propiedades
   configurables, no direcciones IP.
3. **Configuración:** archivos YAML versionados y variables de entorno. Los secretos
   reales se migran a un gestor externo en Fase 10; no se crea un repositorio de
   configuración cifrada.
4. Se da por cerrado el pendiente “Config Server + Eureka (Fase 7)” de ADR-002 y se
   referencia este ADR desde el roadmap.

## Alternativas rechazadas

- **Eureka + Config Server:** complejidad operativa desproporcionada para ocho servicios
  con topología estable; el arranque pasaría a depender de dos componentes adicionales.
- **Service mesh (Istio/Linkerd):** resuelve problemas de malla que hoy no existen y
  duplicaría la validación JWT ya implementada en `casl-security`.
- **Configuración remota sin descubrimiento:** mantiene una dependencia externa para un
  conjunto de propiedades pequeño y versionado.

## Consecuencias

- (+) Arranque determinista y menor número de contenedores en el perfil `core`.
- (+) Cero coste y coherencia con el stack gratuito elegido en ADR-002.
- (+) Kubernetes cubre descubrimiento y configuración en Fase 10 sin producto nuevo.
- (−) Un cambio de configuración requiere redesplegar o recrear el contenedor; se acepta
  porque el número de servicios y de propiedades es acotado.
- (−) No habrá registro dinámico si en el futuro aparece escalado horizontal intensivo;
  se reevaluaría con un ADR nuevo.

## Verificación

- `docker compose --profile core --profile observability config -q` no declara servicios
  de discovery ni de config.
- ADR-002 y `docs/roadmap.md` referencian esta decisión.
