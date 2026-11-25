# Excepciones de seguridad — CASL

> Estado: plantilla · Responsable: Architect · Última revisión: 2026-11-24

Toda excepción a la política de severidad de
[`security-baseline.md`](security-baseline.md) debe registrarse aquí con:

- **ID del hallazgo:** CVE, regla CodeQL o alerta Dependabot.
- **Justificación técnica:** por qué no es explotable o por qué el fix no es viable.
- **Fecha de expiración:** máximo 90 días desde la aprobación. Sin fecha, la excepción
  no es válida.
- **Contramedida aplicada:** compensación mientras la excepción esté vigente.

| ID | Hallazgo | Justificación | Expiración | Contramedida |
|---|---|---|---|---|
| — | — | — | — | — |

Si la tabla solo contiene la fila de ejemplo, no hay excepciones vigentes.

## Excepciones ejecutables

Las excepciones que una herramienta debe aplicar en CI viven junto a su configuración y
repiten el mismo contrato:

- **Trivy:** `.trivyignore.yaml`, con `id`, `statement` y `expired_at` (ISO 8601) por
  entrada. Sin `expired_at` la entrada no es válida y el escaneo debe fallar.
- **ZAP:** `.zap/rules.tsv` o exclusión en el plan, con el ID de alerta y la justificación
  en la columna de comentario admitida por ZAP.

Cada entrada de esas allowlists tiene una fila equivalente en la tabla de arriba. Cuando
una excepción caduca, se elimina de la allowlist y, si persiste, se reevalúa con una nueva
fecha y contramedida.
