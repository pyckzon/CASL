package com.casl.cart.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Vista anti-corruption del producto del catálogo: solo los datos que el
 * carrito necesita. No es la entidad del catálogo.
 */
public record CatalogProduct(
		UUID id,
		String sku,
		String name,
		BigDecimal unitPrice,
		String currency,
		int availableQuantity,
		String status) {

	public boolean isAvailable(int quantity) {
		return "ACTIVE".equalsIgnoreCase(status) && availableQuantity >= quantity;
	}
}
