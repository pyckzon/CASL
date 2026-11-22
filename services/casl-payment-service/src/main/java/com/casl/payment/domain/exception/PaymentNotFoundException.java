package com.casl.payment.domain.exception;

public class PaymentNotFoundException extends RuntimeException {
	public PaymentNotFoundException(java.util.UUID id) {
		super("No payment with id " + id);
	}
}
