package com.casl.order.adapter.in.web;

import com.casl.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
		UUID orderId,
		UUID userId,
		OrderStatus status,
		BigDecimal totalAmount,
		String currency,
		UUID paymentId,
		String failureReason,
		List<OrderLineResponse> lines,
		Instant createdAt,
		Instant updatedAt) {
}
