# Excepciones de seguridad — CASL

> Estado: activo (8 exenciones de IaC) · Responsable: Architect · Última revisión: 2026-11-26

Toda excepción a la política de severidad de
[`security-baseline.md`](security-baseline.md) debe registrarse aquí con:

- **ID del hallazgo:** CVE, regla CodeQL, alerta Dependabot o regla de Trivy misconfig.
- **Justificación técnica:** por qué no es explotable o por qué el fix no es viable.
- **Fecha de expiración:** máximo 90 días desde la aprobación. Sin fecha, la excepción
  no es válida.
- **Contramedida aplicada:** compensación mientras la excepción esté vigente.

| ID | Hallazgo | Justificación | Expiración | Contramedida |
|---|---|---|---|---|
| KSV-0009 | node-exporter con `hostNetwork` | El subchart necesita ver la red del nodo; no es configurable sin romperlo | 2027-02-24 | DaemonSet upstream con recursos limitado a métricas; revisar al actualizar el chart |
| KSV-0010 | node-exporter con `hostPID` | Necesario para métricas de procesos del nodo | 2027-02-24 | Sin privileged ni escalation; revisar al actualizar el chart |
| KSV-0014 | `readOnlyRootFilesystem` ausente en pod de prueba de Loki y en Tempo | Heredado de los charts upstream; el código CASL propio sí lo declara | 2027-02-24 | Contenedores sin privilegios y montajes acotados; `loki.test.enabled=false` |
| KSV-0041 | RBAC con acceso a secrets en Grafana, Prometheus Operator, kube-state-metrics y Loki | Permisos estándar de esos subcharts para leer su configuración | 2027-02-24 | Namespace `casl`, sin cluster-admin; revisar RBAC tras cada upgrade |
| KSV-0056 | Prometheus Operator con permisos de escritura en services/endpoints/ingresses | Requerido para descubrir y gestionar targets del stack | 2027-02-24 | Operador limitado al release `casl`; revisar al actualizar el chart |
| KSV-0114 | Webhook de admisión modifica webhookconfigurations | Es la función del subchart de Prometheus Operator | 2027-02-24 | Webhook del release, sin acceso a secretos CASL; revisar al actualizar |
| KSV-0118 | Pod de prueba de Loki con security context por defecto | Generado por el chart durante `helm test`, no es un workload en producción | 2027-02-24 | Test efímero y sin privilegios; `loki.test.enabled=false` en valores |
| KSV-0121 | node-exporter monta `/`, `/proc` y `/sys` del host | Necesario para recoger métricas del nodo | 2027-02-24 | Montajes de solo lectura del chart; revisar al actualizar |

Las ocho exenciones se aprobaron con el cierre de Fase 10 (PR #43, ADR-021 §8.1) y se
revisan con cada actualización mayor de los charts, antes del 2027-02-24.

La credencial dev fija del Postgres local no es una excepción de hallazgo: está documentada
como brecha conocida en `security-baseline.md` §7 y debe desaparecer antes de producción.

## Excepciones ejecutables

Las excepciones que una herramienta debe aplicar en CI viven junto a su configuración y
repiten el mismo contrato:

- **Trivy:** `.trivyignore.yaml`, con `id`, `statement` y `expired_at` (ISO 8601) por
  entrada. Sin `expired_at` la entrada no es válida y el escaneo debe fallar.
- **ZAP:** exclusiones en `.zap/plan.yaml`, con el ID de alerta y su justificación; no hay
  excepciones ZAP vigentes y `.zap/rules.tsv` queda como mecanismo opcional futuro.

Cada entrada de esas allowlists tiene una fila equivalente en la tabla de arriba. Cuando
una excepción caduca, se elimina de la allowlist y, si persiste, se reevalúa con una nueva
fecha y contramedida.
