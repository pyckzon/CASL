package com.casl.order.domain.model;

import java.util.UUID;

public record PaymentResult(
		boolean success,
		UUID paymentId,
		String failureReason) {

	public static PaymentResult approved(UUID paymentId) {
		return new PaymentResult(true, paymentId, null);
	}

	public static PaymentResult rejected(String reason) {
		return new PaymentResult(false, null, reason);
	}
}
