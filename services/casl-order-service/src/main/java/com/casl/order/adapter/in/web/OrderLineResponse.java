package com.casl.order.adapter.in.web;

import com.casl.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderLineResponse(
		UUID productId,
		String sku,
		String name,
		BigDecimal unitPrice,
		String currency,
		int quantity,
		BigDecimal lineTotal) {
}
