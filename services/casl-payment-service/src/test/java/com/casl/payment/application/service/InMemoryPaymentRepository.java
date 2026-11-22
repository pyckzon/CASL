package com.casl.payment.application.service;

import com.casl.payment.domain.model.Payment;
import com.casl.payment.domain.port.PaymentRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryPaymentRepository implements PaymentRepository {
	private final Map<UUID, Payment> payments = new LinkedHashMap<>();
	private final Map<String, Payment> byKey = new LinkedHashMap<>();

	@Override
	public Payment save(Payment payment) {
		payments.put(payment.paymentId(), payment);
		byKey.put(payment.idempotencyKey(), payment);
		return payment;
	}

	@Override
	public Optional<Payment> findById(UUID paymentId) {
		return Optional.ofNullable(payments.get(paymentId));
	}

	@Override
	public Optional<Payment> findByIdempotencyKey(String key) {
		return Optional.ofNullable(byKey.get(key));
	}
}
