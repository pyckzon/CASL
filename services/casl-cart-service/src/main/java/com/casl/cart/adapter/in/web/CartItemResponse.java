package com.casl.cart.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CartItemResponse(
		UUID productId,
		String sku,
		String name,
		BigDecimal unitPrice,
		String currency,
		int quantity,
		BigDecimal lineTotal) {
}
