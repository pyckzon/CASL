package com.casl.shipping.adapter.out.persistence;

import com.casl.shipping.domain.model.ShipmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipments")
public class ShipmentJpaEntity {

	@Id
	private UUID id;

	@Column(name = "order_id", nullable = false, unique = true)
	private UUID orderId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ShipmentStatus status;

	@Column(length = 50)
	private String carrier;

	@Column(name = "tracking_no", length = 100)
	private String trackingNumber;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ShipmentJpaEntity() {
	}

	public ShipmentJpaEntity(UUID id, UUID orderId, ShipmentStatus status, String carrier, String trackingNumber, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.orderId = orderId;
		this.status = status;
		this.carrier = carrier;
		this.trackingNumber = trackingNumber;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public UUID getId() { return id; }
	public UUID getOrderId() { return orderId; }
	public ShipmentStatus getStatus() { return status; }
	public String getCarrier() { return carrier; }
	public String getTrackingNumber() { return trackingNumber; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
