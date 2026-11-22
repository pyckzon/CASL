package com.casl.notification.adapter.out.persistence;

import com.casl.notification.domain.model.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationJpaEntity {

	@Id
	private UUID id;

	@Column(name = "event_id", nullable = false, unique = true)
	private UUID eventId;

	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@Column(name = "aggregate_id", nullable = false, length = 100)
	private String aggregateId;

	@Column(nullable = false, length = 50)
	private String channel;

	@Column(nullable = false, length = 320)
	private String recipient;

	@Column(length = 200)
	private String subject;

	@Column(columnDefinition = "TEXT")
	private String body;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotificationStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected NotificationJpaEntity() {
	}

	public NotificationJpaEntity(UUID id, UUID eventId, String eventType, String aggregateId, String channel,
			String recipient, String subject, String body, NotificationStatus status, Instant createdAt) {
		this.id = id;
		this.eventId = eventId;
		this.eventType = eventType;
		this.aggregateId = aggregateId;
		this.channel = channel;
		this.recipient = recipient;
		this.subject = subject;
		this.body = body;
		this.status = status;
		this.createdAt = createdAt;
	}

	public UUID getId() { return id; }
	public UUID getEventId() { return eventId; }
	public String getEventType() { return eventType; }
	public String getAggregateId() { return aggregateId; }
	public String getChannel() { return channel; }
	public String getRecipient() { return recipient; }
	public String getSubject() { return subject; }
	public String getBody() { return body; }
	public NotificationStatus getStatus() { return status; }
	public Instant getCreatedAt() { return createdAt; }
}
