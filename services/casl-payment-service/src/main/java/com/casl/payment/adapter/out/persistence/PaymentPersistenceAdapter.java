package com.casl.payment.adapter.out.persistence;

import com.casl.payment.domain.exception.PaymentNotFoundException;
import com.casl.payment.domain.model.Payment;
import com.casl.payment.domain.model.PaymentStatus;
import com.casl.payment.domain.port.PaymentRepository;

import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public class PaymentPersistenceAdapter implements PaymentRepository {

	private final PaymentJpaRepository repository;

	public PaymentPersistenceAdapter(PaymentJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Payment save(Payment payment) {
		PaymentJpaEntity entity = new PaymentJpaEntity(
				payment.paymentId(), payment.idempotencyKey(), payment.amount(), payment.currency(),
				payment.status(), payment.failureReason(), payment.createdAt(), payment.updatedAt());
		PaymentJpaEntity saved = repository.saveAndFlush(entity);
		return toDomain(saved);
	}

	@Override
	public Optional<Payment> findById(UUID paymentId) {
		return repository.findById(paymentId).map(this::toDomain);
	}

	@Override
	public Optional<Payment> findByIdempotencyKey(String key) {
		return repository.findByIdempotencyKey(key).map(this::toDomain);
	}

	private Payment toDomain(PaymentJpaEntity e) {
		return new Payment(e.getId(), e.getIdempotencyKey(), e.getAmount(), e.getCurrency(), e.getStatus(),
				e.getFailureReason(), e.getCreatedAt(), e.getUpdatedAt());
	}
}
