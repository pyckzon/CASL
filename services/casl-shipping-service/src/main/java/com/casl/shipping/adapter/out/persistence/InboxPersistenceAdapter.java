package com.casl.shipping.adapter.out.persistence;

import com.casl.shipping.domain.port.InboxRepository;

import java.time.Instant;
import java.util.UUID;

@org.springframework.stereotype.Repository
public class InboxPersistenceAdapter implements InboxRepository {

	private final InboxJpaRepository repository;

	public InboxPersistenceAdapter(InboxJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public boolean alreadyProcessed(UUID eventId) {
		return repository.existsById(eventId);
	}

	@Override
	public void markProcessed(UUID eventId, String eventType) {
		repository.saveAndFlush(new InboxJpaEntity(eventId, eventType, Instant.now()));
	}
}
