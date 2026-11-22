package com.casl.notification.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Notification(
		UUID id,
		UUID eventId,
		String eventType,
		String aggregateId,
		String channel,
		String recipient,
		String subject,
		String body,
		NotificationStatus status,
		Instant createdAt) {

	public Notification {
		id = Objects.requireNonNull(id, "id is required");
		eventId = Objects.requireNonNull(eventId, "eventId is required");
		if (eventType == null || eventType.isBlank()) throw new IllegalArgumentException("eventType is required");
		eventType = eventType.trim();
		if (aggregateId == null || aggregateId.isBlank()) throw new IllegalArgumentException("aggregateId is required");
		aggregateId = aggregateId.trim();
		if (channel == null || channel.isBlank()) throw new IllegalArgumentException("channel is required");
		channel = channel.trim();
		if (recipient == null || recipient.isBlank()) throw new IllegalArgumentException("recipient is required");
		recipient = recipient.trim();
		status = Objects.requireNonNull(status, "status is required");
		createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
	}
}
