package com.casl.order.application.service;

import com.casl.order.application.port.PaymentCaller;
import com.casl.order.domain.model.PaymentRequest;
import com.casl.order.domain.model.PaymentResult;
import com.casl.order.domain.port.PaymentGateway;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Log4j2
@Component
public class ResilientPaymentCaller implements PaymentCaller {

	private final PaymentGateway delegate;

	@Autowired
	@Lazy
	private ResilientPaymentCaller self;

	public ResilientPaymentCaller(PaymentGateway delegate) {
		this.delegate = delegate;
	}

	@Override
	public PaymentResult call(PaymentRequest request) {
		return self.callAsync(request).join();
	}

	@CircuitBreaker(name = "payment", fallbackMethod = "fallback")
	@Retry(name = "payment", fallbackMethod = "fallback")
	@Bulkhead(name = "payment", fallbackMethod = "fallback")
	@TimeLimiter(name = "payment", fallbackMethod = "fallback")
	public CompletableFuture<PaymentResult> callAsync(PaymentRequest request) {
		return CompletableFuture.supplyAsync(() -> delegate.charge(request));
	}

	public CompletableFuture<PaymentResult> fallback(PaymentRequest request, Throwable throwable) {
		log.warn("Payment fallback for order {}: {}", request.orderId(), throwable.toString());
		return CompletableFuture.completedFuture(
				PaymentResult.rejected("payment service unavailable: " + throwable.getClass().getSimpleName()));
	}
}
