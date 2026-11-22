package com.casl.order.domain.port;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(UUID id, String aggregateId, String eventType, String payload, com.casl.order.domain.outbox.OutboxStatus status, Instant createdAt) {
}
