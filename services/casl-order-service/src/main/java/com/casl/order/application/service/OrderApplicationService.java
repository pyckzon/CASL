package com.casl.order.application.service;

import com.casl.order.application.command.CancelOrderCommand;
import com.casl.order.application.command.CreateOrderCommand;
import com.casl.order.application.port.in.OrderUseCase;
import com.casl.order.application.port.PaymentCaller;
import com.casl.order.domain.exception.OrderNotFoundException;
import com.casl.order.domain.model.Order;
import com.casl.order.domain.model.PaymentRequest;
import com.casl.order.domain.model.PaymentResult;
import com.casl.order.domain.port.OrderRepository;
import com.casl.order.domain.port.OutboxRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Log4j2
@Service
@Transactional(readOnly = true)
public class OrderApplicationService implements OrderUseCase {

	private final OrderRepository repository;
	private final PaymentCaller paymentCaller;
	private final OutboxRepository outboxRepository;
	private final tools.jackson.databind.ObjectMapper objectMapper;
	private final Clock clock;

	public OrderApplicationService(OrderRepository repository, PaymentCaller paymentCaller, OutboxRepository outboxRepository,
			tools.jackson.databind.ObjectMapper objectMapper, Clock clock) {
		this.repository = repository;
		this.paymentCaller = paymentCaller;
		this.outboxRepository = outboxRepository;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Order create(CreateOrderCommand command) {
		Instant now = clock.instant();
		Order order = Order.create(command.userId(), command.lines(), now);
		Order saved = repository.save(order);
		// Transactional outbox: persist event in same transaction
		try {
			com.casl.order.domain.event.EventEnvelope envelope = new com.casl.order.domain.event.EventEnvelope(
					java.util.UUID.randomUUID(), "order.created.v1", saved.orderId().toString(),
					saved.createdAt(), 1, null, saved);
			outboxRepository.save(java.util.UUID.randomUUID(), saved.orderId().toString(), "order.created.v1",
					objectMapper.writeValueAsString(envelope), saved.createdAt());
		} catch (Exception exception) {
			log.error("Failed to write outbox event for order {}", saved.orderId(), exception);
		}
		log.info("Order created id={} total={}", saved.orderId(), saved.totalAmount());
		return saved;
	}

	@Override
	public Order getById(UUID orderId) {
		return repository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
	}

	@Override
	@Transactional
	public Order processPayment(UUID orderId) {
		Order order = getById(orderId);
		if (order.status() == com.casl.order.domain.model.OrderStatus.PAID) {
			return order;
		}
		if (order.status() != com.casl.order.domain.model.OrderStatus.CREATED
				&& order.status() != com.casl.order.domain.model.OrderStatus.PAYMENT_PENDING) {
			throw new IllegalStateException("Order is not payable: " + order.status());
		}
		Instant now = clock.instant();
		Order pending = order.markPaymentPending(now);
		repository.save(pending);

		PaymentRequest request = new PaymentRequest(orderId, order.totalAmount(), order.currency(),
				orderId.toString());
		PaymentResult result;
		try {
			result = paymentCaller.call(request);
		} catch (Exception exception) {
			log.error("Payment call failed for order {}", orderId, exception);
			result = PaymentResult.rejected("payment service unavailable");
		}

		if (result.success()) {
			Order paid = pending.markPaid(result.paymentId(), clock.instant());
			Order saved = repository.save(paid);
			log.info("Order paid id={} paymentId={}", saved.orderId(), result.paymentId());
			return saved;
		}
		Order failed = pending.markPaymentFailed(result.failureReason(), clock.instant()).cancel(result.failureReason(), clock.instant());
		Order saved = repository.save(failed);
		log.warn("Order payment failed id={} reason={}", saved.orderId(), result.failureReason());
		return saved;
	}

	@Override
	@Transactional
	public Order cancel(CancelOrderCommand command) {
		Order order = getById(command.orderId());
		Order cancelled = order.cancel(command.reason() == null ? "cancelled by user" : command.reason(), clock.instant());
		Order saved = repository.save(cancelled);
		log.info("Order cancelled id={} reason={}", saved.orderId(), saved.failureReason());
		return saved;
	}
}
