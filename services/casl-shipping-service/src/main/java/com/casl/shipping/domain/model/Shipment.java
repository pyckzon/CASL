package com.casl.shipping.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Shipment(
		UUID shipmentId,
		UUID orderId,
		ShipmentStatus status,
		String carrier,
		String trackingNumber,
		Instant createdAt,
		Instant updatedAt) {

	public Shipment {
		shipmentId = Objects.requireNonNull(shipmentId, "shipmentId is required");
		orderId = Objects.requireNonNull(orderId, "orderId is required");
		status = Objects.requireNonNull(status, "status is required");
		createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
		updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
	}

	public static Shipment create(UUID orderId, Instant now) {
		return new Shipment(UUID.randomUUID(), orderId, ShipmentStatus.CREATED, null, null, now, now);
	}

	public Shipment dispatch(String carrier, String trackingNumber, Instant now) {
		return new Shipment(shipmentId, orderId, ShipmentStatus.DISPATCHED, carrier, trackingNumber, createdAt, now);
	}

	public Shipment deliver(Instant now) {
		return new Shipment(shipmentId, orderId, ShipmentStatus.DELIVERED, carrier, trackingNumber, createdAt, now);
	}

	public Shipment cancel(Instant now) {
		return new Shipment(shipmentId, orderId, ShipmentStatus.CANCELLED, carrier, trackingNumber, createdAt, now);
	}
}
