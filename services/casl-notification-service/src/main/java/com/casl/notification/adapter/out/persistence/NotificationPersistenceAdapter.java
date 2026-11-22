package com.casl.notification.adapter.out.persistence;

import com.casl.notification.domain.model.Notification;
import com.casl.notification.domain.port.InboxRepository;
import com.casl.notification.domain.port.NotificationRepository;

import java.time.Instant;
import java.util.UUID;

@org.springframework.stereotype.Repository
public class NotificationPersistenceAdapter implements NotificationRepository, InboxRepository {

	private final NotificationJpaRepository notificationRepository;
	private final InboxJpaRepository inboxRepository;

	public NotificationPersistenceAdapter(NotificationJpaRepository notificationRepository, InboxJpaRepository inboxRepository) {
		this.notificationRepository = notificationRepository;
		this.inboxRepository = inboxRepository;
	}

	@Override
	public Notification save(Notification notification) {
		NotificationJpaEntity entity = new NotificationJpaEntity(
				notification.id(), notification.eventId(), notification.eventType(), notification.aggregateId(),
				notification.channel(), notification.recipient(), notification.subject(), notification.body(),
				notification.status(), notification.createdAt());
		notificationRepository.saveAndFlush(entity);
		return notification;
	}

	@Override
	public boolean existsByEventId(UUID eventId) {
		return notificationRepository.existsByEventId(eventId);
	}

	@Override
	public boolean alreadyProcessed(UUID eventId) {
		return inboxRepository.existsById(eventId);
	}

	@Override
	public void markProcessed(UUID eventId, String eventType) {
		inboxRepository.saveAndFlush(new InboxJpaEntity(eventId, eventType, Instant.now()));
	}
}
