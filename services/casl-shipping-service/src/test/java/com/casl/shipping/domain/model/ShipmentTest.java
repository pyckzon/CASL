package com.casl.shipping.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentTest {

	private static final UUID ORDER_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

	@Test
	void createSetsCreatedStatus() {
		Shipment s = Shipment.create(ORDER_ID, NOW);
		assertThat(s.status()).isEqualTo(ShipmentStatus.CREATED);
		assertThat(s.orderId()).isEqualTo(ORDER_ID);
	}

	@Test
	void dispatchSetsDispatchedWithTracking() {
		Shipment s = Shipment.create(ORDER_ID, NOW).dispatch("fixed-rate", "TRK-123", NOW.plusSeconds(10));
		assertThat(s.status()).isEqualTo(ShipmentStatus.DISPATCHED);
		assertThat(s.carrier()).isEqualTo("fixed-rate");
		assertThat(s.trackingNumber()).isEqualTo("TRK-123");
	}

	@Test
	void deliverSetsDelivered() {
		Shipment s = Shipment.create(ORDER_ID, NOW).dispatch("c", "t", NOW).deliver(NOW.plusSeconds(1));
		assertThat(s.status()).isEqualTo(ShipmentStatus.DELIVERED);
	}
}
