# ADR-003 — Arquitectura hexagonal por servicio

- **Estado:** Aceptado
- **Fecha:** 2026-11-21
- **Decisores:** Architect

## Contexto

El servicio `casl-users-service` nació como un esqueleto con controlador, modelos y utilidades en un solo paquete. El objetivo del sistema exige que cada servicio pueda **extenderse sin modificarse** (OCP): cambiar de proveedor de pago, de canal de notificación, de sink de telemetría o de base de datos no debe tocar las reglas de negocio. Además, con ~10 servicios, el coste de un diseño acoplado se multiplica.

## Decisión

Cada servicio se organiza en cuatro zonas con una regla de dependencia estricta hacia dentro:

```
adapter/in  ──▶ application ──▶ domain
adapter/out ──▶ application ──▶ domain   (vía puertos)
```

| Zona | Contenido | Reglas |
|---|---|---|
| `domain` | Agregados, value objects, invariantes, excepciones de negocio, **puertos** | Java puro: sin Spring, JPA, Jackson ni Kafka. Sin dependencias externas |
| `application` | Casos de uso, orquestación, transacciones | Depende solo del dominio; define puertos de entrada |
| `adapter/in` | Controllers REST, DTOs (`record`), mapeo web, manejo de errores | Traduce HTTP ↔ casos de uso |
| `adapter/out` | JPA, Kafka, clientes HTTP, caché | Implementa los puertos de salida |
| `config` | Cableado de beans | Único lugar que conoce todas las implementaciones |

Reglas adicionales:

1. **La entidad JPA nunca sale del adaptador de persistencia.** El dominio y la API tienen sus propios tipos.
2. **DTOs y comandos son `record`** (inmutables). Las clases se reservan para entidades con comportamiento o estado mutable.
3. **Un puerto se crea cuando existe una segunda implementación real o comprometida**, no por especulación (YAGNI).
4. **Los tests de dominio no arrancan Spring**: son JUnit puro y corren en milisegundos.

## Estructura concreta (users)

```
com.casl.users
├── domain
│   ├── model/User.java, UserStatus.java
│   ├── exception/UserNotFoundException.java, EmailAlreadyExistsException.java, InvalidUserException.java
│   └── port/UserRepository.java          # puerto de salida
├── application
│   ├── port/in/…UseCase.java             # puertos de entrada
│   └── service/UserApplicationService.java
├── adapter
│   ├── in/web/UserController.java, dto/, mapper/, error/
│   └── out/persistence/UserJpaEntity.java, UserJpaRepository.java, UserPersistenceAdapter.java
└── config/UserConfiguration.java
```

## Consecuencias

- (+) El dominio es testeable sin infraestructura y sobrevive a cambios de framework.
- (+) Añadir un adaptador (otra BD, otro canal) no modifica casos de uso.
- (+) Límites explícitos entre contextos; el acoplamiento se ve en el código.
- (−) Más clases y mapeos que un diseño en capas simple.
- (−) Riesgo de sobre-abstracción si se crean puertos sin segunda implementación; mitigado por la regla 3.
- Verificación futura: test de arquitectura (ArchUnit) que impida imports de `org.springframework` en `domain`.

## Alternativas consideradas

- **Capas clásicas (controller → service → repository):** más rápido al inicio, pero el dominio queda acoplado a JPA y el OCP se pierde.
- **Clean Architecture completa (casos de uso + interactors + presenters):** más ceremonial del necesario para este tamaño; hexagonal cubre el objetivo con menos capas.
- **Sin puertos:** válido para un CRUD trivial, insuficiente para un portfolio que debe demostrar extensibilidad.
