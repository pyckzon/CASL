package com.casl.payment.domain.port;

import java.math.BigDecimal;

/**
 * Pasarela de pago simulada (Adapter). Sustituible por Stripe/PayPal/MercadoPago.
 */
public interface PaymentProcessor {
	PaymentProcessorResult process(BigDecimal amount, String currency);
}
