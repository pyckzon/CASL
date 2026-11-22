package com.casl.shipping.application.service;

import com.casl.shipping.domain.model.Shipment;
import com.casl.shipping.domain.model.ShipmentStatus;
import com.casl.shipping.domain.port.ShipmentRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryShipmentRepository implements ShipmentRepository {
	private final Map<UUID, Shipment> shipments = new LinkedHashMap<>();

	@Override
	public Shipment save(Shipment shipment) {
		shipments.put(shipment.shipmentId(), shipment);
		return shipment;
	}

	@Override
	public Optional<Shipment> findById(UUID shipmentId) {
		return Optional.ofNullable(shipments.get(shipmentId));
	}

	@Override
	public Optional<Shipment> findByOrderId(UUID orderId) {
		return shipments.values().stream().filter(s -> s.orderId().equals(orderId)).findFirst();
	}

	@Override
	public boolean existsByOrderId(UUID orderId) {
		return findByOrderId(orderId).isPresent();
	}
}

class FakeShippingProvider implements com.casl.shipping.domain.port.ShippingProvider {
	@Override
	public String providerName() { return "fake"; }
	@Override
	public String dispatch(com.casl.shipping.domain.model.Shipment shipment) { return "TRK-FAKE"; }
}
