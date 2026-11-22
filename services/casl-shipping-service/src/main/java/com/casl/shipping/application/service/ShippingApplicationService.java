package com.casl.shipping.application.service;

import com.casl.shipping.application.port.in.ShippingUseCase;
import com.casl.shipping.domain.model.Shipment;
import com.casl.shipping.domain.port.ShipmentRepository;
import com.casl.shipping.domain.port.ShippingProvider;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Log4j2
@Service
@Transactional(readOnly = true)
public class ShippingApplicationService implements ShippingUseCase {

	private final ShipmentRepository repository;
	private final ShippingProvider provider;
	private final Clock clock;

	public ShippingApplicationService(ShipmentRepository repository, ShippingProvider provider, Clock clock) {
		this.repository = repository;
		this.provider = provider;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Shipment createShipment(UUID orderId) {
		return repository.findByOrderId(orderId).orElseGet(() -> {
			Instant now = clock.instant();
			Shipment shipment = Shipment.create(orderId, now);
			Shipment saved = repository.save(shipment);
			log.info("Shipment created id={} orderId={}", saved.shipmentId(), orderId);
			return saved;
		});
	}

	@Override
	@Transactional
	public Shipment dispatch(UUID shipmentId) {
		Shipment shipment = repository.findById(shipmentId)
				.orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));
		String tracking = provider.dispatch(shipment);
		Shipment dispatched = shipment.dispatch(provider.providerName(), tracking, clock.instant());
		Shipment saved = repository.save(dispatched);
		log.info("Shipment dispatched id={} carrier={} tracking={}", saved.shipmentId(), saved.carrier(), saved.trackingNumber());
		return saved;
	}

	@Override
	@Transactional
	public Shipment deliver(UUID shipmentId) {
		Shipment shipment = repository.findById(shipmentId)
				.orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));
		Shipment delivered = shipment.deliver(clock.instant());
		return repository.save(delivered);
	}

	@Override
	public Shipment getByOrderId(UUID orderId) {
		return repository.findByOrderId(orderId)
				.orElseThrow(() -> new IllegalArgumentException("No shipment for order: " + orderId));
	}
}
