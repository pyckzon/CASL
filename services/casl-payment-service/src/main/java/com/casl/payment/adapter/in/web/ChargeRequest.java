package com.casl.payment.adapter.in.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ChargeRequest(
		@NotNull @DecimalMin("0.00") BigDecimal amount,
		@NotBlank @Size(min = 3, max = 3) String currency,
		@NotBlank String idempotencyKey) {
}
