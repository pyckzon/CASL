package com.casl.shipping.adapter.out.persistence;

import com.casl.shipping.domain.model.Shipment;
import com.casl.shipping.domain.model.ShipmentStatus;
import com.casl.shipping.domain.port.ShipmentRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public class ShipmentPersistenceAdapter implements ShipmentRepository {

	private final ShipmentJpaRepository repository;

	public ShipmentPersistenceAdapter(ShipmentJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Shipment save(Shipment shipment) {
		ShipmentJpaEntity entity = new ShipmentJpaEntity(
				shipment.shipmentId(), shipment.orderId(), shipment.status(), shipment.carrier(),
				shipment.trackingNumber(), shipment.createdAt(), shipment.updatedAt());
		repository.saveAndFlush(entity);
		return shipment;
	}

	@Override
	public Optional<Shipment> findById(UUID shipmentId) {
		return repository.findById(shipmentId).map(this::toDomain);
	}

	@Override
	public Optional<Shipment> findByOrderId(UUID orderId) {
		return repository.findByOrderId(orderId).map(this::toDomain);
	}

	@Override
	public boolean existsByOrderId(UUID orderId) {
		return repository.existsByOrderId(orderId);
	}

	private Shipment toDomain(ShipmentJpaEntity e) {
		return new Shipment(e.getId(), e.getOrderId(), e.getStatus(), e.getCarrier(), e.getTrackingNumber(),
				e.getCreatedAt(), e.getUpdatedAt());
	}
}
