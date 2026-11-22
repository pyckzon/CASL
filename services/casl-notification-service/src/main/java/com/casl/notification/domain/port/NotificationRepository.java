package com.casl.notification.domain.port;

import com.casl.notification.domain.model.Notification;

import java.util.UUID;

public interface NotificationRepository {
	Notification save(Notification notification);
	boolean existsByEventId(UUID eventId);
}
