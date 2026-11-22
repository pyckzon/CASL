package com.casl.shipping.application.service;

import com.casl.shipping.domain.model.Shipment;
import com.casl.shipping.domain.model.ShipmentStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShippingApplicationServiceTest {

	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
	private static final UUID ORDER_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");

	private final InMemoryShipmentRepository repository = new InMemoryShipmentRepository();
	private final ShippingApplicationService service = new ShippingApplicationService(repository, new FakeShippingProvider(),
			Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void createShipmentIsIdempotentByOrder() {
		Shipment first = service.createShipment(ORDER_ID);
		Shipment second = service.createShipment(ORDER_ID);

		assertThat(second.shipmentId()).isEqualTo(first.shipmentId());
		assertThat(repository.findByOrderId(ORDER_ID)).isPresent();
	}

	@Test
	void dispatchUpdatesStatusAndTracking() {
		Shipment created = service.createShipment(ORDER_ID);

		Shipment dispatched = service.dispatch(created.shipmentId());

		assertThat(dispatched.status()).isEqualTo(ShipmentStatus.DISPATCHED);
		assertThat(dispatched.carrier()).isEqualTo("fake");
		assertThat(dispatched.trackingNumber()).isEqualTo("TRK-FAKE");
	}

	@Test
	void getByOrderId() {
		Shipment created = service.createShipment(ORDER_ID);

		assertThat(service.getByOrderId(ORDER_ID)).isEqualTo(created);
	}
}
