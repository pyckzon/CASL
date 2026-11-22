package com.casl.order.application.service;

import com.casl.order.domain.model.PaymentRequest;
import com.casl.order.domain.model.PaymentResult;
import com.casl.order.domain.port.PaymentGateway;

class FakePaymentGateway implements PaymentGateway {
	private boolean approve = true;
	private String failureReason = "rejected";

	void setApprove(boolean approve) { this.approve = approve; }
	void setFailureReason(String reason) { this.failureReason = reason; }

	@Override
	public PaymentResult charge(PaymentRequest request) {
		if (approve) {
			return PaymentResult.approved(java.util.UUID.randomUUID());
		}
		return PaymentResult.rejected(failureReason);
	}
}
