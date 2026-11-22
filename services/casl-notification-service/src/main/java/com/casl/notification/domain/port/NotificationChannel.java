package com.casl.notification.domain.port;

import com.casl.notification.domain.model.Notification;

/**
 * Canal de notificación (Strategy). Añadir un canal nuevo no modifica el
 * consumidor ni el dominio.
 */
public interface NotificationChannel {
	String channelName();
	void send(Notification notification);
}
