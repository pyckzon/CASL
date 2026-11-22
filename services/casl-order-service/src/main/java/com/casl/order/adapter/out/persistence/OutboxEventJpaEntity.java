package com.casl.order.adapter.out.persistence;

import com.casl.order.domain.outbox.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox")
public class OutboxEventJpaEntity {

	@Id
	private UUID id;

	@Column(name = "aggregate_id", nullable = false, length = 100)
	private String aggregateId;

	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OutboxStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "published_at")
	private Instant publishedAt;

	protected OutboxEventJpaEntity() {
	}

	public OutboxEventJpaEntity(UUID id, String aggregateId, String eventType, String payload, OutboxStatus status, Instant createdAt) {
		this.id = id;
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.payload = payload;
		this.status = status;
		this.createdAt = createdAt;
	}

	public UUID getId() { return id; }
	public String getAggregateId() { return aggregateId; }
	public String getEventType() { return eventType; }
	public String getPayload() { return payload; }
	public OutboxStatus getStatus() { return status; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getPublishedAt() { return publishedAt; }

	public void markPublished(Instant at) {
		this.status = OutboxStatus.PUBLISHED;
		this.publishedAt = at;
	}
}
