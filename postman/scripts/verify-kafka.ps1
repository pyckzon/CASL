# Comprueba, fuera de Postman, las garantias que el CLI no puede observar:
# entrega de eventos, idempotencia de notification (inbox) y publicacion en DLT.
[CmdletBinding()]
param(
  [int] $TimeoutMs = 10000,
  [int] $WaitSeconds = 3
)

$ErrorActionPreference = 'Stop'

function Assert-Condition([bool] $Condition, [string] $Message) {
  if (-not $Condition) { throw "FAIL $Message" }
  Write-Host "OK   $Message"
}

function Read-Topic([string] $Topic) {
  $lines = docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh `
    --bootstrap-server kafka:9092 --topic $Topic --from-beginning --timeout-ms $TimeoutMs 2>$null
  return [string]::Join("`n", $lines)
}

function Write-Topic([string] $Topic, [string] $Line) {
  $Line | docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server kafka:9092 --topic $Topic | Out-Null
}

function Read-Notification([string] $Sql) {
  return (docker compose exec -T postgres-notifications psql -U casl -d casl_notifications -tAc $Sql).Trim()
}

# 1. Entrega de eventos de la saga.
Assert-Condition ((Read-Topic 'order.created.v1') -match 'order.created.v1') `
  'order.created.v1 observable en Kafka'
Assert-Condition ((Read-Topic 'payment.approved.v1') -match 'payment.approved.v1') `
  'payment.approved.v1 observable en Kafka'

# 2. notification es idempotente por eventId (inbox + UNIQUE).
$eventId = [guid]::NewGuid().ToString()
$envelope = '{"eventId":"' + $eventId + '","eventType":"order.created.v1",' +
  '"aggregateId":"phase8-e2e","occurredAt":"2026-11-24T00:00:00Z",' +
  '"schemaVersion":1,"traceId":null,"payload":{}}'
$before = [int](Read-Notification "select count(*) from notifications where event_id = '$eventId'")
Assert-Condition ($before -eq 0) 'evento de prueba no procesado antes del envio'
Write-Topic 'order.created.v1' $envelope
Start-Sleep -Seconds $WaitSeconds
$first = [int](Read-Notification "select count(*) from notifications where event_id = '$eventId'")
Assert-Condition ($first -ge 1) "evento entregado y persistido por al menos un canal (filas=$first)"
Write-Topic 'order.created.v1' $envelope
Start-Sleep -Seconds $WaitSeconds
$after = [int](Read-Notification "select count(*) from notifications where event_id = '$eventId'")
Assert-Condition ($after -eq $first) "reintento del evento no duplica notificacion (primera=$first, despues=$after)"
$inbox = [int](Read-Notification "select count(*) from inbox where event_id = '$eventId'")
Assert-Condition ($inbox -eq 1) 'inbox registra el evento exactamente una vez'

# 3. Un registro malformado agota reintentos y llega al DLT de shipping.
Write-Topic 'payment.approved.v1' 'mensaje-malformado-phase8'
Start-Sleep -Seconds (2 * $WaitSeconds)
$dlt = Read-Topic 'payment.approved.v1.DLT'
Assert-Condition ($dlt -match 'mensaje-malformado-phase8') `
  'registro malformado publicado en payment.approved.v1.DLT'

Write-Host 'Verificacion Kafka de la Fase 8 completada'
