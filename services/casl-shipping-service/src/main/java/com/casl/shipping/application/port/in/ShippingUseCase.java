package com.casl.shipping.application.port.in;

import com.casl.shipping.domain.model.Shipment;

import java.util.UUID;

public interface ShippingUseCase {
	Shipment createShipment(UUID orderId);
	Shipment dispatch(UUID shipmentId);
	Shipment deliver(UUID shipmentId);
	Shipment getByOrderId(UUID orderId);
}
