package com.casl.cart.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Item del carrito. Inmutable: las operaciones producen un item nuevo.
 */
public record CartItem(
		UUID productId,
		String sku,
		String name,
		BigDecimal unitPrice,
		String currency,
		int quantity) {

	public CartItem {
		productId = Objects.requireNonNull(productId, "productId is required");
		if (sku == null || sku.isBlank()) {
			throw new IllegalArgumentException("sku is required");
		}
		sku = sku.trim().toUpperCase();
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("name is required");
		}
		name = name.trim();
		Objects.requireNonNull(unitPrice, "unitPrice is required");
		if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("unitPrice must not be negative");
		}
		if (currency == null || currency.isBlank()) {
			throw new IllegalArgumentException("currency is required");
		}
		currency = currency.trim().toUpperCase();
		if (quantity < 1) {
			throw new IllegalArgumentException("quantity must be at least 1");
		}
	}

	public BigDecimal lineTotal() {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	public CartItem withQuantity(int newQuantity) {
		return new CartItem(productId, sku, name, unitPrice, currency, newQuantity);
	}
}
