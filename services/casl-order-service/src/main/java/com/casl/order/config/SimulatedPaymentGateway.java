package com.casl.order.config;

import com.casl.order.domain.model.PaymentRequest;
import com.casl.order.domain.model.PaymentResult;
import com.casl.order.domain.port.PaymentGateway;

import java.math.BigDecimal;

/**
 * Pasarela de pago simulada y determinista: aprueba cobros hasta un límite
 * configurable y rechaza el resto. Permite probar la saga sin pasarela real.
 * Sustituible por un adaptador REST hacia payment-service sin tocar el dominio.
 */
public class SimulatedPaymentGateway implements PaymentGateway {

	private final BigDecimal approvalLimit;

	public SimulatedPaymentGateway(BigDecimal approvalLimit) {
		this.approvalLimit = approvalLimit;
	}

	public SimulatedPaymentGateway() {
		this(new BigDecimal("1000.00"));
	}

	@Override
	public PaymentResult charge(PaymentRequest request) {
		if (request.amount().compareTo(approvalLimit) <= 0) {
			return PaymentResult.approved(java.util.UUID.randomUUID());
		}
		return PaymentResult.rejected("amount exceeds approval limit of " + approvalLimit);
	}
}
