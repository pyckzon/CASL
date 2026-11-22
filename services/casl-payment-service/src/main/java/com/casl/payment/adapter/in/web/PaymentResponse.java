package com.casl.payment.adapter.in.web;

import com.casl.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
		UUID paymentId,
		String idempotencyKey,
		BigDecimal amount,
		String currency,
		PaymentStatus status,
		String failureReason,
		Instant createdAt,
		Instant updatedAt) {
}
