package com.casl.order.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Order(
		UUID orderId,
		UUID userId,
		List<OrderLine> lines,
		OrderStatus status,
		BigDecimal totalAmount,
		String currency,
		UUID paymentId,
		String failureReason,
		Instant createdAt,
		Instant updatedAt) {

	public Order {
		orderId = Objects.requireNonNull(orderId, "orderId is required");
		lines = List.copyOf(lines);
		status = Objects.requireNonNull(status, "status is required");
		totalAmount = Objects.requireNonNull(totalAmount, "totalAmount is required");
		if (currency == null || currency.isBlank()) throw new IllegalArgumentException("currency is required");
		currency = currency.trim().toUpperCase();
		createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
		updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
	}

	public static Order create(UUID userId, List<OrderLine> lines, Instant now) {
		BigDecimal total = lines.stream().map(OrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		String currency = lines.isEmpty() ? "USD" : lines.get(0).currency();
		return new Order(UUID.randomUUID(), userId, lines, OrderStatus.CREATED, total, currency, null, null, now, now);
	}

	public Order markPaymentPending(Instant now) {
		return new Order(orderId, userId, lines, OrderStatus.PAYMENT_PENDING, totalAmount, currency, paymentId, failureReason, createdAt, now);
	}

	public Order markPaid(UUID paymentId, Instant now) {
		return new Order(orderId, userId, lines, OrderStatus.PAID, totalAmount, currency, paymentId, null, createdAt, now);
	}

	public Order markPaymentFailed(String reason, Instant now) {
		return new Order(orderId, userId, lines, OrderStatus.PAYMENT_FAILED, totalAmount, currency, null, reason, createdAt, now);
	}

	public Order cancel(String reason, Instant now) {
		return new Order(orderId, userId, lines, OrderStatus.CANCELLED, totalAmount, currency, paymentId, reason, createdAt, now);
	}
}
