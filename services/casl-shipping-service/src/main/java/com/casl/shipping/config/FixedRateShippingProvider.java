package com.casl.shipping.config;

import com.casl.shipping.domain.port.ShippingProvider;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Proveedor de envío inicial: tarifa fija y tracking simulado.
 * Sustituible por transportistas reales (OCP).
 */
@Component
public class FixedRateShippingProvider implements ShippingProvider {

	@Override
	public String providerName() {
		return "fixed-rate";
	}

	@Override
	public String dispatch(com.casl.shipping.domain.model.Shipment shipment) {
		return "TRK-" + shipment.shipmentId().toString().substring(0, 8).toUpperCase();
	}
}
