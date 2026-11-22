package com.casl.payment.domain.port;

import com.casl.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
	Payment save(Payment payment);
	Optional<Payment> findById(UUID paymentId);
	Optional<Payment> findByIdempotencyKey(String key);
}
