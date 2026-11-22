package com.casl.payment.application.service;

import com.casl.payment.application.command.ChargeCommand;
import com.casl.payment.application.port.in.PaymentUseCase;
import com.casl.payment.domain.exception.PaymentNotFoundException;
import com.casl.payment.domain.model.Payment;
import com.casl.payment.domain.model.PaymentStatus;
import com.casl.payment.domain.port.PaymentProcessor;
import com.casl.payment.domain.port.PaymentProcessorResult;
import com.casl.payment.domain.port.PaymentRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Log4j2
@Service
@Transactional(readOnly = true)
public class PaymentApplicationService implements PaymentUseCase {

	private final PaymentRepository repository;
	private final PaymentProcessor processor;
	private final Clock clock;

	public PaymentApplicationService(PaymentRepository repository, PaymentProcessor processor, Clock clock) {
		this.repository = repository;
		this.processor = processor;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Payment charge(ChargeCommand command) {
		// Idempotencia: si ya existe un pago con esta clave, devolverlo sin recobrar.
		return repository.findByIdempotencyKey(command.idempotencyKey())
				.orElseGet(() -> processNew(command));
	}

	private Payment processNew(ChargeCommand command) {
		Instant now = clock.instant();
		Payment pending = Payment.pending(command.idempotencyKey(), command.amount(), command.currency(), now);
		Payment saved = repository.save(pending);

		PaymentProcessorResult result = processor.process(command.amount(), command.currency());
		Payment updated;
		if (result.approved()) {
			updated = saved.approve(clock.instant());
			log.info("Payment approved id={} key={}", updated.paymentId(), command.idempotencyKey());
		} else {
			updated = saved.reject(result.failureReason(), clock.instant());
			log.warn("Payment rejected key={} reason={}", command.idempotencyKey(), result.failureReason());
		}
		return repository.save(updated);
	}

	@Override
	public Payment getById(UUID paymentId) {
		return repository.findById(paymentId).orElseThrow(() -> new PaymentNotFoundException(paymentId));
	}

	@Override
	@Transactional
	public Payment refund(UUID paymentId) {
		Payment payment = getById(paymentId);
		Payment refunded = payment.refund(clock.instant());
		Payment saved = repository.save(refunded);
		log.info("Payment refunded id={}", saved.paymentId());
		return saved;
	}
}
