package com.casl.order.domain.port;

import com.casl.order.domain.model.PaymentRequest;
import com.casl.order.domain.model.PaymentResult;

/**
 * Puerto de pago (Adapter). El simulador y el cliente REST hacia payment-service
 * implementan este puerto sin tocar el dominio.
 */
public interface PaymentGateway {
	PaymentResult charge(PaymentRequest request);
}
