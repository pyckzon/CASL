package com.casl.order.adapter.out.persistence;

import com.casl.order.domain.outbox.OutboxStatus;
import com.casl.order.domain.port.OutboxEvent;
import com.casl.order.domain.port.OutboxRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Repository
public class OutboxPersistenceAdapter implements OutboxRepository {

	private final OutboxJpaRepository repository;

	public OutboxPersistenceAdapter(OutboxJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public void save(UUID id, String aggregateId, String eventType, String payload, Instant createdAt) {
		repository.saveAndFlush(new OutboxEventJpaEntity(id, aggregateId, eventType, payload, OutboxStatus.PENDING, createdAt));
	}

	@Override
	public List<OutboxEvent> findPending(int limit) {
		return repository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING,
				org.springframework.data.domain.PageRequest.of(0, limit))
				.stream()
				.map(e -> new OutboxEvent(e.getId(), e.getAggregateId(), e.getEventType(), e.getPayload(), e.getStatus(), e.getCreatedAt()))
				.collect(Collectors.toList());
	}

	@Override
	public void markPublished(UUID id, Instant publishedAt) {
		repository.findById(id).ifPresent(e -> {
			e.markPublished(publishedAt);
			repository.save(e);
		});
	}
}
