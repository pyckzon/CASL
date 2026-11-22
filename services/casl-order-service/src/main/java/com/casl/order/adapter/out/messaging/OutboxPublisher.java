package com.casl.order.adapter.out.messaging;

import com.casl.order.domain.event.EventEnvelope;
import com.casl.order.domain.port.OutboxEvent;
import com.casl.order.domain.port.OutboxRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxPublisher {

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, String> kafkaTemplate;

	public OutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
		this.outboxRepository = outboxRepository;
		this.kafkaTemplate = kafkaTemplate;
	}

	@Scheduled(fixedDelay = 5000)
	public void publishPending() {
		List<OutboxEvent> pending = outboxRepository.findPending(50);
		for (OutboxEvent event : pending) {
			try {
				kafkaTemplate.send(event.eventType(), event.aggregateId(), event.payload()).get();
				outboxRepository.markPublished(event.id(), Instant.now());
			} catch (Exception exception) {
				// Leave as PENDING for next retry
			}
		}
	}
}
