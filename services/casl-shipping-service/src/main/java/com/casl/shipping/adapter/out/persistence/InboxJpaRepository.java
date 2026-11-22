package com.casl.shipping.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InboxJpaRepository extends JpaRepository<InboxJpaEntity, UUID> {
	boolean existsById(UUID eventId);
}
