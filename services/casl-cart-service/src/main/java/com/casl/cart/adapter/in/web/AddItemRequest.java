package com.casl.cart.adapter.in.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AddItemRequest(
		UUID productId,
		String sku,
		@Min(1) int quantity) {
}
