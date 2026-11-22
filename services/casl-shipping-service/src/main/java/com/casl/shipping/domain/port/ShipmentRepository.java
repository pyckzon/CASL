package com.casl.shipping.domain.port;

import com.casl.shipping.domain.model.Shipment;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository {
	Shipment save(Shipment shipment);
	Optional<Shipment> findById(UUID shipmentId);
	Optional<Shipment> findByOrderId(UUID orderId);
	boolean existsByOrderId(UUID orderId);
}
