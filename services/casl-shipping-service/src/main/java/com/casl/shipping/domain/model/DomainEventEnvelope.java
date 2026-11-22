package com.casl.shipping.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DomainEventEnvelope(
		UUID eventId,
		String eventType,
		String aggregateId,
		Instant occurredAt,
		int schemaVersion,
		String traceId,
		Object payload) {

	public DomainEventEnvelope {
		eventId = Objects.requireNonNull(eventId, "eventId is required");
		if (eventType == null || eventType.isBlank()) throw new IllegalArgumentException("eventType is required");
		eventType = eventType.trim();
		if (aggregateId == null || aggregateId.isBlank()) throw new IllegalArgumentException("aggregateId is required");
		aggregateId = aggregateId.trim();
		occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
	}
}
