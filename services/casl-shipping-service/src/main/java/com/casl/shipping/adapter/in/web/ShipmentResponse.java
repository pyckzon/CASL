package com.casl.shipping.adapter.in.web;

import com.casl.shipping.domain.model.Shipment;
import com.casl.shipping.domain.model.ShipmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ShipmentResponse(
		UUID shipmentId,
		UUID orderId,
		ShipmentStatus status,
		String carrier,
		String trackingNumber,
		Instant createdAt,
		Instant updatedAt) {
}
