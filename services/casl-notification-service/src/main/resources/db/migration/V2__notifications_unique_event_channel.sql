-- Un evento puede entregarse por varios canales (email, log, webhook).
-- V1 declaró UNIQUE(event_id), por lo que el segundo canal violaba la restricción
-- y el evento acababa en el DLT antes de marcar el inbox como procesado.
ALTER TABLE notifications DROP CONSTRAINT uk_notifications_event_id;
ALTER TABLE notifications ADD CONSTRAINT uk_notifications_event_channel UNIQUE (event_id, channel);
