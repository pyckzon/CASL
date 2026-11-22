package com.casl.notification.application.service;

import com.casl.notification.domain.model.DomainEventEnvelope;
import com.casl.notification.domain.model.Notification;
import com.casl.notification.domain.model.NotificationStatus;
import com.casl.notification.domain.port.InboxRepository;
import com.casl.notification.domain.port.NotificationChannel;
import com.casl.notification.domain.port.NotificationRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final InboxRepository inboxRepository;
	private final List<NotificationChannel> channels;
	private final Clock clock;

	public NotificationService(NotificationRepository notificationRepository, InboxRepository inboxRepository,
			List<NotificationChannel> channels, Clock clock) {
		this.notificationRepository = notificationRepository;
		this.inboxRepository = inboxRepository;
		this.channels = channels;
		this.clock = clock;
	}

	public void process(DomainEventEnvelope event) {
		if (inboxRepository.alreadyProcessed(event.eventId())) {
			log.info("Event {} already processed, skipping", event.eventId());
			return;
		}

		String recipient = resolveRecipient(event);
		for (NotificationChannel channel : channels) {
			Notification notification = new Notification(
					UUID.randomUUID(),
					event.eventId(),
					event.eventType(),
					event.aggregateId(),
					channel.channelName(),
					recipient,
					subjectFor(event),
					bodyFor(event),
					NotificationStatus.PENDING,
					Instant.now(clock));
			try {
				channel.send(notification);
				notificationRepository.save(new Notification(
						notification.id(), notification.eventId(), notification.eventType(), notification.aggregateId(),
						notification.channel(), notification.recipient(), notification.subject(), notification.body(),
						NotificationStatus.SENT, notification.createdAt()));
			} catch (Exception exception) {
				log.error("Failed to send notification via {}", channel.channelName(), exception);
				notificationRepository.save(new Notification(
						notification.id(), notification.eventId(), notification.eventType(), notification.aggregateId(),
						notification.channel(), notification.recipient(), notification.subject(), notification.body(),
						NotificationStatus.FAILED, notification.createdAt()));
			}
		}

		inboxRepository.markProcessed(event.eventId(), event.eventType());
		log.info("Processed event {} type={}", event.eventId(), event.eventType());
	}

	private String resolveRecipient(DomainEventEnvelope event) {
		Object payload = event.payload();
		if (payload instanceof java.util.Map) {
			Object email = ((java.util.Map<?, ?>) payload).get("email");
			if (email != null) return String.valueOf(email);
		}
		return "user@" + event.aggregateId() + ".local";
	}

	private String subjectFor(DomainEventEnvelope event) {
		return "Notification: " + event.eventType();
	}

	private String bodyFor(DomainEventEnvelope event) {
		return "Event " + event.eventType() + " for aggregate " + event.aggregateId();
	}
}
