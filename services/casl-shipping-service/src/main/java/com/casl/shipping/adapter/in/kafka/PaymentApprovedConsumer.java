package com.casl.shipping.adapter.in.kafka;

import com.casl.shipping.application.service.ShippingApplicationService;
import com.casl.shipping.domain.model.DomainEventEnvelope;
import com.casl.shipping.domain.model.Shipment;
import com.casl.shipping.domain.port.InboxRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Log4j2
@Component
public class PaymentApprovedConsumer {

	private final ShippingApplicationService shippingService;
	private final InboxRepository inboxRepository;
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final tools.jackson.databind.ObjectMapper objectMapper;

	public PaymentApprovedConsumer(ShippingApplicationService shippingService, InboxRepository inboxRepository,
			KafkaTemplate<String, String> kafkaTemplate, tools.jackson.databind.ObjectMapper objectMapper) {
		this.shippingService = shippingService;
		this.inboxRepository = inboxRepository;
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "payment.approved.v1", groupId = "${spring.kafka.consumer.group-id}")
	public void onPaymentApproved(DomainEventEnvelope event) {
		if (event == null || inboxRepository.alreadyProcessed(event.eventId())) {
			log.info("Event {} already processed or null, skipping", event == null ? "null" : event.eventId());
			return;
		}

		try {
			UUID orderId = UUID.fromString(event.aggregateId());
			Shipment shipment = shippingService.createShipment(orderId);
			Shipment dispatched = shippingService.dispatch(shipment.shipmentId());

			// Publish shipment.dispatched.v1
			com.casl.shipping.domain.model.DomainEventEnvelope envelope = new com.casl.shipping.domain.model.DomainEventEnvelope(
					java.util.UUID.randomUUID(), "shipment.dispatched.v1", orderId.toString(),
					Instant.now(), 1, event.traceId(),
					java.util.Map.of("shipmentId", dispatched.shipmentId().toString(), "orderId", orderId.toString(),
							"carrier", dispatched.carrier(), "trackingNumber", dispatched.trackingNumber()));
			String payload = objectMapper.writeValueAsString(envelope);
			kafkaTemplate.send("shipment.dispatched.v1", orderId.toString(), payload);

			inboxRepository.markProcessed(event.eventId(), event.eventType());
			log.info("Shipment dispatched for order {} tracking={}", orderId, dispatched.trackingNumber());
		} catch (Exception exception) {
			log.error("Failed to process payment.approved event {}", event.eventId(), exception);
		}
	}
}
