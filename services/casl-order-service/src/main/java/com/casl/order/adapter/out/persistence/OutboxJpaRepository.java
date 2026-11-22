package com.casl.order.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {
	List<OutboxEventJpaEntity> findByStatusOrderByCreatedAtAsc(com.casl.order.domain.outbox.OutboxStatus status, org.springframework.data.domain.Pageable pageable);
}
