package com.casl.payment.application.service;

import com.casl.payment.domain.port.PaymentProcessor;
import com.casl.payment.domain.port.PaymentProcessorResult;

import java.math.BigDecimal;

class FakePaymentProcessor implements PaymentProcessor {
	private boolean approve = true;
	private String failureReason = "rejected";

	void setApprove(boolean approve) { this.approve = approve; }

	@Override
	public PaymentProcessorResult process(BigDecimal amount, String currency) {
		if (approve) {
			return PaymentProcessorResult.approved(java.util.UUID.randomUUID());
		}
		return PaymentProcessorResult.rejected(failureReason);
	}
}
