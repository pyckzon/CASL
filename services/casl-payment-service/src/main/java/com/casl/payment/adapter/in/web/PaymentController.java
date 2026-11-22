package com.casl.payment.adapter.in.web;

import com.casl.payment.application.command.ChargeCommand;
import com.casl.payment.application.port.in.PaymentUseCase;
import com.casl.payment.domain.model.Payment;
import com.casl.security.ServiceToken;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments/v1/payments")
public class PaymentController {

	private static final String BASE = "/api/payments/v1/payments/";
	private static final String ORDER_SERVICE_CLIENT_ID = "casl-order-service";

	private final PaymentUseCase useCase;

	public PaymentController(PaymentUseCase useCase) {
		this.useCase = useCase;
	}

	@PostMapping
	public ResponseEntity<PaymentResponse> charge(
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody ChargeRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		ServiceToken.requireManager(jwt, ORDER_SERVICE_CLIENT_ID);
		Payment payment = useCase.charge(new ChargeCommand(request.amount(), request.currency(), idempotencyKey));
		return ResponseEntity.created(URI.create(BASE + payment.paymentId()))
				.body(toResponse(payment));
	}

	@GetMapping("/{paymentId}")
	public PaymentResponse get(@PathVariable UUID paymentId, @AuthenticationPrincipal Jwt jwt) {
		ServiceToken.requireManager(jwt, ORDER_SERVICE_CLIENT_ID);
		return toResponse(useCase.getById(paymentId));
	}

	@PostMapping("/{paymentId}/refund")
	public PaymentResponse refund(@PathVariable UUID paymentId, @AuthenticationPrincipal Jwt jwt) {
		ServiceToken.requireManager(jwt, ORDER_SERVICE_CLIENT_ID);
		return toResponse(useCase.refund(paymentId));
	}

	private static PaymentResponse toResponse(Payment payment) {
		return new PaymentResponse(payment.paymentId(), payment.idempotencyKey(), payment.amount(), payment.currency(),
				payment.status(), payment.failureReason(), payment.createdAt(), payment.updatedAt());
	}
}
