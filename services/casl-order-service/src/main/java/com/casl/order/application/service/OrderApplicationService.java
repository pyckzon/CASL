package com.casl.order.application.service;

import com.casl.order.application.command.CancelOrderCommand;
import com.casl.order.application.command.CreateOrderCommand;
import com.casl.order.application.port.in.OrderUseCase;
import com.casl.order.domain.exception.OrderNotFoundException;
import com.casl.order.domain.model.Order;
import com.casl.order.domain.model.PaymentRequest;
import com.casl.order.domain.model.PaymentResult;
import com.casl.order.domain.port.OrderRepository;
import com.casl.order.domain.port.PaymentGateway;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Log4j2
@Service
@Transactional(readOnly = true)
public class OrderApplicationService implements OrderUseCase {

	private final OrderRepository repository;
	private final PaymentGateway paymentGateway;
	private final Clock clock;

	public OrderApplicationService(OrderRepository repository, PaymentGateway paymentGateway, Clock clock) {
		this.repository = repository;
		this.paymentGateway = paymentGateway;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Order create(CreateOrderCommand command) {
		Instant now = clock.instant();
		Order order = Order.create(command.userId(), command.lines(), now);
		Order saved = repository.save(order);
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
			result = chargePayment(request).join();
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

	@CircuitBreaker(name = "payment", fallbackMethod = "paymentFallback")
	@Retry(name = "payment", fallbackMethod = "paymentFallback")
	@Bulkhead(name = "payment", fallbackMethod = "paymentFallback")
	@TimeLimiter(name = "payment", fallbackMethod = "paymentFallback")
	public java.util.concurrent.CompletableFuture<PaymentResult> chargePayment(PaymentRequest request) {
		return java.util.concurrent.CompletableFuture.supplyAsync(() -> paymentGateway.charge(request));
	}

	public java.util.concurrent.CompletableFuture<PaymentResult> paymentFallback(PaymentRequest request, Throwable throwable) {
		log.warn("Payment fallback for order {}: {}", request.orderId(), throwable.toString());
		return java.util.concurrent.CompletableFuture.completedFuture(
				PaymentResult.rejected("payment service unavailable: " + throwable.getClass().getSimpleName()));
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
