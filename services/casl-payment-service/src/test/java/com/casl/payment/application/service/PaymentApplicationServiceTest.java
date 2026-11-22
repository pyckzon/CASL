package com.casl.payment.application.service;

import com.casl.payment.application.command.ChargeCommand;
import com.casl.payment.domain.exception.PaymentNotFoundException;
import com.casl.payment.domain.model.Payment;
import com.casl.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentApplicationServiceTest {

	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

	private final InMemoryPaymentRepository repository = new InMemoryPaymentRepository();
	private final FakePaymentProcessor processor = new FakePaymentProcessor();
	private final PaymentApplicationService service = new PaymentApplicationService(repository, processor,
			Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void chargeApprovesAndPersists() {
		Payment payment = service.charge(new ChargeCommand(new BigDecimal("9.99"), "USD", "key-1"));

		assertThat(payment.paymentId()).isNotNull();
		assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);
		assertThat(payment.idempotencyKey()).isEqualTo("key-1");
		assertThat(repository.findById(payment.paymentId())).contains(payment);
	}

	@Test
	void chargeRejectsWhenProcessorRejects() {
		processor.setApprove(false);
		Payment payment = service.charge(new ChargeCommand(new BigDecimal("9.99"), "USD", "key-2"));

		assertThat(payment.status()).isEqualTo(PaymentStatus.REJECTED);
	}

	@Test
	void chargeIsIdempotentByKey() {
		Payment first = service.charge(new ChargeCommand(new BigDecimal("9.99"), "USD", "key-3"));
		Payment second = service.charge(new ChargeCommand(new BigDecimal("9.99"), "USD", "key-3"));

		assertThat(second.paymentId()).isEqualTo(first.paymentId());
		assertThat(second.status()).isEqualTo(first.status());
	}

	@Test
	void getByIdReturnsOrThrows() {
		Payment created = service.charge(new ChargeCommand(new BigDecimal("9.99"), "USD", "key-4"));

		assertThat(service.getById(created.paymentId())).isEqualTo(created);
		UUID unknown = UUID.randomUUID();
		assertThatThrownBy(() -> service.getById(unknown)).isInstanceOf(PaymentNotFoundException.class);
	}

	@Test
	void refundMarksRefunded() {
		Payment approved = service.charge(new ChargeCommand(new BigDecimal("9.99"), "USD", "key-5"));

		Payment refunded = service.refund(approved.paymentId());

		assertThat(refunded.status()).isEqualTo(PaymentStatus.REFUNDED);
	}

	@Test
	void refundOnNonApprovedThrows() {
		processor.setApprove(false);
		Payment rejected = service.charge(new ChargeCommand(new BigDecimal("9.99"), "USD", "key-6"));

		assertThatThrownBy(() -> service.refund(rejected.paymentId()))
				.isInstanceOf(IllegalStateException.class);
	}
}
