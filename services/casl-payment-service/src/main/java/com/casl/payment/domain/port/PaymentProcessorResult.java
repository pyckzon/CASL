package com.casl.payment.domain.port;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentProcessorResult(boolean approved, UUID paymentId, String failureReason) {
	public static PaymentProcessorResult approved(UUID paymentId) {
		return new PaymentProcessorResult(true, paymentId, null);
	}
	public static PaymentProcessorResult rejected(String reason) {
		return new PaymentProcessorResult(false, null, reason);
	}
}
