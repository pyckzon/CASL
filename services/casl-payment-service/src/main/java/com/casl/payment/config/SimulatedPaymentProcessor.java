package com.casl.payment.config;

import com.casl.payment.domain.port.PaymentProcessor;
import com.casl.payment.domain.port.PaymentProcessorResult;

import java.math.BigDecimal;

public class SimulatedPaymentProcessor implements PaymentProcessor {

	private final BigDecimal approvalLimit;

	public SimulatedPaymentProcessor() {
		this(new BigDecimal("1000.00"));
	}

	public SimulatedPaymentProcessor(BigDecimal approvalLimit) {
		this.approvalLimit = approvalLimit;
	}

	@Override
	public PaymentProcessorResult process(BigDecimal amount, String currency) {
		if (amount.compareTo(approvalLimit) <= 0) {
			return PaymentProcessorResult.approved(java.util.UUID.randomUUID());
		}
		return PaymentProcessorResult.rejected("amount exceeds approval limit of " + approvalLimit);
	}
}
