package com.casl.shipping.adapter.in.web;

import com.casl.shipping.application.port.in.ShippingUseCase;
import com.casl.shipping.domain.model.Shipment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/shipping/v1/shipments")
public class ShipmentController {

	private static final String BASE = "/api/shipping/v1/shipments/";

	private final ShippingUseCase useCase;

	public ShipmentController(ShippingUseCase useCase) {
		this.useCase = useCase;
	}

	@GetMapping("/order/{orderId}")
	public ShipmentResponse getByOrder(@PathVariable UUID orderId) {
		return toResponse(useCase.getByOrderId(orderId));
	}

	@PostMapping("/{shipmentId}/dispatch")
	public ShipmentResponse dispatch(@PathVariable UUID shipmentId) {
		return toResponse(useCase.dispatch(shipmentId));
	}

	@PostMapping("/{shipmentId}/deliver")
	public ShipmentResponse deliver(@PathVariable UUID shipmentId) {
		return toResponse(useCase.deliver(shipmentId));
	}

	private static ShipmentResponse toResponse(Shipment s) {
		return new ShipmentResponse(s.shipmentId(), s.orderId(), s.status(), s.carrier(), s.trackingNumber(),
				s.createdAt(), s.updatedAt());
	}
}
