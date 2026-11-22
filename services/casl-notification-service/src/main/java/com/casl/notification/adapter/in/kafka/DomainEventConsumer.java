package com.casl.notification.adapter.in.kafka;

import com.casl.notification.application.service.NotificationService;
import com.casl.notification.domain.model.DomainEventEnvelope;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class DomainEventConsumer {

	private final NotificationService notificationService;

	public DomainEventConsumer(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@KafkaListener(topics = { "order.created.v1", "user.created.v1", "payment.approved.v1", "payment.rejected.v1" }, groupId = "${spring.kafka.consumer.group-id}")
	public void onEvent(DomainEventEnvelope event) {
		if (event == null) {
			log.warn("Received null event, skipping");
			return;
		}
		log.info("Received event {} type={} aggregate={}", event.eventId(), event.eventType(), event.aggregateId());
		notificationService.process(event);
	}
}
