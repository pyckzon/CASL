package com.casl.cart.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
		UUID cartId,
		UUID userId,
		List<CartItemResponse> items,
		int totalItems,
		BigDecimal totalAmount,
		Instant createdAt,
		Instant updatedAt) {
}
