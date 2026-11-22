package com.casl.order.application.port;

import com.casl.order.domain.model.PaymentRequest;
import com.casl.order.domain.model.PaymentResult;

/**
 * Puerto de llamada a la pasarela de pago. La implementación resiliente
 * (ResilientPaymentCaller) aplica circuit breaker, retry, timeout y bulkhead.
 */
public interface PaymentCaller {

	PaymentResult call(PaymentRequest request);
}
