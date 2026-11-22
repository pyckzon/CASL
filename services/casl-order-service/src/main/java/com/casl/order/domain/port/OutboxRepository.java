package com.casl.order.domain.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository {
	void save(UUID id, String aggregateId, String eventType, String payload, Instant createdAt);
	List<OutboxEvent> findPending(int limit);
	void markPublished(UUID id, Instant publishedAt);
}
