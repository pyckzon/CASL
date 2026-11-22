package com.casl.shipping.domain.port;

import com.casl.shipping.domain.model.Shipment;

/**
 * Proveedor de envío (Strategy). Añadir transportistas reales no modifica el dominio.
 */
public interface ShippingProvider {
	String providerName();
	String dispatch(Shipment shipment);
}
