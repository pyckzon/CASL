package com.casl.order.adapter.out.persistence;

import com.casl.order.domain.port.OutboxEvent;
import com.casl.order.domain.port.OutboxRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryOutboxRepository implements OutboxRepository {
	private final List<OutboxEvent> events = new ArrayList<>();

	@Override
	public void save(UUID id, String aggregateId, String eventType, String payload, Instant createdAt) {
		events.add(new OutboxEvent(id, aggregateId, eventType, payload, com.casl.order.domain.outbox.OutboxStatus.PENDING, createdAt));
	}

	@Override
	public List<OutboxEvent> findPending(int limit) {
		return events.stream().filter(e -> e.status() == com.casl.order.domain.outbox.OutboxStatus.PENDING).limit(limit).toList();
	}

	@Override
	public void markPublished(UUID id, Instant publishedAt) {
		// no-op for tests
	}
}
