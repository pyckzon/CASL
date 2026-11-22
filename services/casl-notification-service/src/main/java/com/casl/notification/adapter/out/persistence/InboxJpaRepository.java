package com.casl.notification.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface InboxJpaRepository extends JpaRepository<InboxJpaEntity, UUID> {
	boolean existsById(UUID eventId);
}
