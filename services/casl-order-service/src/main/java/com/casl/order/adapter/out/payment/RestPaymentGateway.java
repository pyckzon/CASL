package com.casl.order.adapter.out.payment;

import com.casl.order.domain.model.PaymentRequest;
import com.casl.order.domain.model.PaymentResult;
import com.casl.order.domain.port.PaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

public class RestPaymentGateway implements PaymentGateway {

	private final RestClient restClient;

	public RestPaymentGateway(RestClient.Builder builder, @Value("${casl.payment.base-url:http://localhost:8673}") String baseUrl) {
		this.restClient = builder.baseUrl(baseUrl).build();
	}

	@Override
	public PaymentResult charge(PaymentRequest request) {
		try {
			PaymentResponse response = restClient.post()
					.uri("/api/payments/v1/payments")
					.header("Idempotency-Key", request.idempotencyKey())
					.body(new ChargePayload(request.amount(), request.currency(), request.idempotencyKey()))
					.retrieve()
					.body(PaymentResponse.class);
			if (response == null) {
				throw new IllegalStateException("empty response from payment service");
			}
			if ("APPROVED".equalsIgnoreCase(response.status())) {
				return PaymentResult.approved(response.paymentId());
			}
			return PaymentResult.rejected(response.failureReason() == null ? "payment rejected" : response.failureReason());
		} catch (HttpClientErrorException exception) {
			if (exception.getStatusCode().is4xxClientError()) {
				return PaymentResult.rejected("payment rejected: " + exception.getMessage());
			}
			throw exception;
		}
	}

	public record ChargePayload(java.math.BigDecimal amount, String currency, String idempotencyKey) {
	}

	public record PaymentResponse(UUID paymentId, String status, String failureReason) {
	}
}
