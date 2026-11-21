package com.casl.catalog.config;

import com.casl.catalog.domain.port.StockPolicy;

/**
 * Política de stock inicial: reserva optimista (verifica disponibilidad antes
 * de reservar). Sustituible por políticas pesimistas o de backorder sin tocar
 * el dominio (OCP).
 */
public class OptimisticStockPolicy implements StockPolicy {

	@Override
	public boolean canReserve(com.casl.catalog.domain.model.Product product, int quantity) {
		return quantity > 0 && product.availableQuantity() >= quantity;
	}
}
