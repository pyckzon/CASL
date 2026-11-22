package com.casl.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Payment(
		UUID paymentId,
		String idempotencyKey,
		BigDecimal amount,
		String currency,
		PaymentStatus status,
		String failureReason,
		Instant createdAt,
		Instant updatedAt) {

	public Payment {
		paymentId = Objects.requireNonNull(paymentId, "paymentId is required");
		if (idempotencyKey == null || idempotencyKey.isBlank()) throw new IllegalArgumentException("idempotencyKey is required");
		idempotencyKey = idempotencyKey.trim();
		Objects.requireNonNull(amount, "amount is required");
		if (amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("amount must not be negative");
		if (currency == null || currency.isBlank()) throw new IllegalArgumentException("currency is required");
		currency = currency.trim().toUpperCase();
		status = Objects.requireNonNull(status, "status is required");
		createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
		updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
	}

	public static Payment pending(String idempotencyKey, BigDecimal amount, String currency, Instant now) {
		return new Payment(UUID.randomUUID(), idempotencyKey, amount, currency, PaymentStatus.PENDING, null, now, now);
	}

	public Payment approve(Instant now) {
		return new Payment(paymentId, idempotencyKey, amount, currency, PaymentStatus.APPROVED, null, createdAt, now);
	}

	public Payment reject(String reason, Instant now) {
		return new Payment(paymentId, idempotencyKey, amount, currency, PaymentStatus.REJECTED, reason, createdAt, now);
	}

	public Payment refund(Instant now) {
		if (status != PaymentStatus.APPROVED) {
			throw new IllegalStateException("Only approved payments can be refunded");
		}
		return new Payment(paymentId, idempotencyKey, amount, currency, PaymentStatus.REFUNDED, null, createdAt, now);
	}
}
