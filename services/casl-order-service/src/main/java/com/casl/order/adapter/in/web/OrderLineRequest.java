package com.casl.order.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderLineRequest(
		@NotNull UUID productId,
		@NotBlank @Size(max = 64) String sku,
		@NotBlank @Size(max = 200) String name,
		@NotNull BigDecimal unitPrice,
		@NotBlank @Size(min = 3, max = 3) String currency,
		@Min(1) int quantity) {
}
