package com.casl.order.domain.event;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
		UUID eventId,
		String eventType,
		String aggregateId,
		Instant occurredAt,
		int schemaVersion,
		String traceId,
		Object payload) {
}
